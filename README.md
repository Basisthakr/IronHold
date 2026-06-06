# IronHold

A privacy-focused, end-to-end encrypted file storage and sharing backend. Files are encrypted client-side before upload. The server stores only encrypted blobs in S3 and never has access to plaintext content. Auth is passwordless, based on Ed25519 key pairs.

## Stack

- Spring Boot 4.0.6, Java 21, Maven
- PostgreSQL 15.16
- AWS S3 (SDK v2.25.60)
- JWT (jjwt 0.12.6)
- Lombok, SpringDoc OpenAPI

## Implemented

**Auth**
- Passwordless registration with Ed25519 public key
- Challenge/sign/verify flow for login, returns a JWT
- Token revocation on logout
- Account recovery via one-time recovery keys (BCrypt-hashed, 8 keys per rotation)
- Public key rotation (invalidates all previously issued JWTs)

**Files**
- Two-step encrypted upload: server issues a presigned S3 PUT URL, client uploads directly to S3
- Presigned download URLs with correct Content-Disposition so browsers save files with original filenames.
- Soft delete and hard delete (hard delete removes DB record and S3 object)
- File sharing with per-recipient encrypted file keys and configurable permission levels (READ, READ_WRITE, SHARED_OWNER)
- Optional share expiry
- Storage quota enforcement per user (5 GB)
- pagination on file listings 

**Folders**
- Create folders with optional parent (nested folder support)
- List folders by parent
- Soft delete a folder (cascades to all subfolders and files inside, revokes related FileShares)
- Folder sharing: creates FolderShare and FileShares for all files currently in the folder
- pagination on file listings 

**Misc**
- Audit log model in place
- Demo account cleanup: soft-deletes demo files after 30 minutes, hard-deletes demo accounts nightly at 3 AM IST
- Interactive playground hosted at `https://basistth.dev/ironhold-playground`

## In Progress
 
·  Redis caching for quota and presigned URLs  
·  scheduled background jobs (orphaned blob cleanup, expired share revocation)  
·  multipart uploads for large files  
·  OpenAPI/Swagger documentation 

## Deployment

- API at `https://ironhold.basistth.dev`
