# StegaCrypt

StegaCrypt is a full-stack steganography web application that encrypts text and hides it inside images. It combines public-key cryptography, randomized LSB embedding, and a built-in secure chat workflow in one project.

The project was designed as a practical mini-project that brings together cryptography, secure communication, and image processing in a way that is easy to demonstrate from the browser. Instead of only showing a basic hide-and-extract flow, StegaCrypt also includes a secure chat experience where authenticated users can share stego images with specific recipients. That makes the project useful not just as a steganography demo, but as a complete proof-of-concept for confidential message exchange using images as the transport layer.

At the core of the system, the backend encrypts every message with a fresh AES session key, protects that key with RSA-OAEP, and embeds the encrypted payload into image pixels using a randomized LSB strategy. The frontend then exposes that workflow through a guided interface for embedding, extraction, key management, and secure member-to-member message sharing. The result is a project that demonstrates how modern encryption and steganography can be combined in a real full-stack application.

## Highlights

- RSA public/private key workflow for safer message sharing
- AES-256-GCM for message encryption with integrity protection
- RSA-OAEP wrapping for one-time AES session keys
- Randomized LSB steganography for image embedding
- Capacity checking before embedding
- Secure chat with login, registration, demo members, and recipient-based decryption
- React frontend and Spring Boot backend

## How It Works

### Embed

1. Upload a carrier image.
2. Generate or paste the recipient public key.
3. Encrypt the message with a fresh AES session key.
4. Wrap that key with RSA.
5. Embed the payload into the image and export a PNG stego file.

### Extract

1. Upload the stego image.
2. Provide the matching RSA private key or use the secure chat flow.
3. Rebuild the deterministic embedding path.
4. Recover and decrypt the hidden message.

## Tech Stack

- Frontend: React, Vite, CSS
- Backend: Spring Boot, Java 17, Maven
- Crypto: RSA-2048, RSA-OAEP, AES-256-GCM
- Image processing: randomized LSB steganography

## Project Modules

- `frontend/` contains the full user interface for embedding, extraction, and secure chat actions.
- `backend/` contains the Spring Boot API that handles cryptography, image processing, secure chat state, and validation.
- `docs/` stores the detailed project documentation, setup notes, API guide, deployment guide, and academic report.
- `run-project.bat` and `run-project.sh` provide helper scripts for quickly starting the whole project.

## Main Use Cases

- Hide encrypted text inside a carrier image and export a stego PNG.
- Extract a hidden message using the matching private key or key file.
- Check image capacity before embedding a message.
- Register or log in as a secure chat user and send hidden image-based messages to a selected recipient.
- Demonstrate a hybrid security model that combines encryption, integrity protection, and steganographic concealment.

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000` after both services are running.

## Documentation

- [Quick Start](docs/QUICKSTART.md)
- [API Docs](docs/API_DOCS.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Run Guide and User Manual](docs/RUN_ME.md)
- [Project Report](docs/PROJECT_REPORT.md)
- [Hosting and Project Working](docs/HOSTING_AND_PROJECT_WORKING.md)

## Important Notes

- Keep the private key or generated key file safe.
- Use the exported PNG output for reliable extraction.
- Avoid editing or recompressing the stego image after embedding.
- Secure chat data is runtime-backed and may reset when the backend restarts.

## Academic Context

This project was developed as an MIT-WPU mini project for the 2025-2026 academic year.
