# IronHold

A privacy-focused, end-to-end encrypted file storage and sharing backend. Files are encrypted on the client before upload. The server stores only encrypted blobs in S3 and never has access to plaintext content or encryption keys. Authentication is passwordless, based on Ed25519 public key cryptography.

---

## Architecture

### Authentication

IronHold uses a challenge/sign/verify flow instead of passwords.

1. The client registers with an Ed25519 public key. No password is stored.
2. To log in, the client requests a challenge: the server generates a random nonce, stores it with a 120-second expiry, and returns it.
3. The client signs the nonce with its Ed25519 private key and sends the signature back.
4. The server verifies the signature against the stored public key using `X509EncodedKeySpec` + `Signature.getInstance("Ed25519")`. On success, it issues a JWT (HMAC-SHA, 1-hour expiry).
5. JWTs are stateless but can be explicitly revoked: logout writes the token to a `revoked_tokens` table, and `JwtFilter` checks every request against it.
6. Public key rotation immediately invalidates all previously issued JWTs by recording a `keyRotatedAt` timestamp on the user. `JwtFilter` rejects any token with an `iat` before that timestamp.
7. Account recovery uses 8 one-time recovery keys generated at registration and on each rotation. They are stored as BCrypt hashes and never in plaintext.

### File Storage

Uploads are a two-step process that keeps the server out of the data path entirely.

1. The client calls `POST /api/file/upload` with file metadata. The server checks the storage quota, creates a `StoredFile` record (status `WAITING`), and returns a 15-minute presigned S3 PUT URL.
2. The client encrypts the file locally, then PUTs the encrypted bytes directly to S3 using the presigned URL. No Spring Boot involvement in the upload itself.
3. The client calls `POST /api/file/upload/complete`. The server verifies the blob exists via `headObject`, sets the status to `DONE`, and updates the user's `quotaUsed`.

Downloads follow the same pattern: the server issues a presigned GET URL (via `GET /api/file/download?fileId=`) with a `Content-Disposition` header so browsers save files with the original filename rather than the S3 object key.

S3 object keys follow the format `{userId}/{fileId}.enc`. Path-style access is enabled on the S3 client and presigner so both produce consistent canonical request signatures.

### Encryption Key Distribution

The server never sees plaintext encryption keys. When a file is shared, the sharer encrypts the file's AES key with each recipient's Ed25519 public key and sends the ciphertext to the server. The server stores one `encryptedFileKey` per `FileShare` row. Each recipient decrypts it client-side with their private key.

### Sharing

File sharing is record-based. A `FileShare` row ties a file to a recipient with a permission level (`READ`, `READ_WRITE`, or `SHARED_OWNER`), an optional expiry, and the recipient's encrypted file key. Folder sharing creates a `FolderShare` and a `FileShare` for every file currently in the folder. Files uploaded into a shared folder after sharing automatically inherit the folder's share configuration via `isUploadInSharedFolder`.

### Delete Flow

Deletes are two-stage. Soft delete sets `isDeleted = true` and records `deletedAt`; the S3 object and DB record are kept and the file remains visible in listings. Hard delete permanently removes the DB record and the S3 object and is only available for files already soft-deleted.

### Caching

Download URL generation is cached in Redis (`@Cacheable`, cache name `downloadUrls`, 14-minute TTL). The TTL is intentionally longer than the default because presigned S3 GET URLs are valid for 15 minutes, so a cached URL stays useful for most of its lifetime. The `CacheManager` is configured in `RedisConfig` with a 10-minute default TTL for any other caches added in future.

### Scheduled Jobs

Two background jobs run on a schedule:

- Every 5 minutes: soft-deletes demo files (`demo_*` accounts) older than 30 minutes and removes the corresponding S3 objects.
- Nightly at 3 AM IST: hard-deletes demo accounts older than 24 hours, cascading through FileShares, FolderShares, files, folders, recovery keys, and auth challenges in FK-safe order.

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.6, Maven |
| Database | PostgreSQL 15.16 |
| Cache | Redis (Spring Data Redis, Lettuce) |
| Object Storage | AWS S3 (SDK v2.25.60), bucket `ironhold-main`, `ap-south-1` |
| Auth | Ed25519 (JCA), JWT via jjwt 0.12.6 |
| Utilities | Lombok, SpringDoc OpenAPI |

---

## API Overview

### Auth (`/api/auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/register` | Public | Register with username + Ed25519 public key. Returns userId and 8 recovery keys. |
| POST | `/challenge` | Public | Request a nonce for the given userId. |
| POST | `/verify` | Public | Submit a signed nonce. Returns a JWT on success. |
| POST | `/logout` | JWT | Revoke the current token. |
| POST | `/recover` | Public | Recover account with a one-time recovery key, rotate public key, get new JWT. |
| POST | `/rotate-recovery-keys` | JWT | Invalidate existing recovery keys and generate 8 new ones. |
| POST | `/rotate-public-key` | JWT | Replace the public key, invalidating all current JWTs. |

### Files (`/api/file`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/upload` | JWT | Initiate upload. Returns fileId and presigned S3 PUT URL. |
| POST | `/upload/complete` | JWT | Confirm upload, update quota, auto-create FileShares for shared folders. |
| GET | `/download` | JWT | Returns presigned S3 GET URL. Cached in Redis. |
| GET | `/list` | JWT | List files in a folder (paginated). Includes soft-deleted files. |
| DELETE | `/delete` | JWT | Soft-delete a file. |
| POST | `/harddelete` | JWT | Permanently delete soft-deleted files and their S3 objects. |
| POST | `/share` | JWT | Share a file with another user, providing their encrypted file key. |

### Folders (`/api/folder`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/create` | JWT | Create a folder, optionally nested under a parent. |
| GET | `/list` | JWT | List folders by parent. |
| DELETE | `/delete` | JWT | Soft-delete a folder, all subfolders, all contained files, and related shares. |
| POST | `/share` | JWT | Share a folder and create FileShares for all current files in it. |

### Health

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | Public | Returns `{"status": "ok"}`. |

---

## Deployment

- API: `https://ironhold.basistth.dev`
- Server: EC2 t3.small, Amazon Linux 2023, nginx reverse proxy to port 8081
- Systemd service: `ironhold`
- Playground: `https://basistth.dev/ironhold-playground`
