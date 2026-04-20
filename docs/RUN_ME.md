# StegaCrypt Run Guide, User Manual, Deployment Notes, and Code Structure

This file is the all-in-one guide for running, using, deploying, and understanding the StegaCrypt project.

## 1. Project Summary

StegaCrypt is a full-stack web application that hides encrypted text messages inside images.

It has two main parts:

- `frontend/`: React + Vite user interface
- `backend/`: Spring Boot API for cryptography, image processing, steganography, authentication, and secure chat

Main features:

- RSA key-pair generation
- AES-256-GCM message encryption
- RSA-OAEP wrapping of the AES session key
- Randomized LSB-based image steganography
- Image capacity check before embedding
- Key-file based extraction
- Secure chat with login, registration, seeded member accounts, and recipient-based decryption

## 2. Software Requirements

Install these before running the project:

- Java `17`
- Maven `3.9+`
- Node.js `18+`
- npm `9+`

Recommended tools:

- Windows Command Prompt or PowerShell for `run-project.bat`
- Git Bash, WSL, or Linux/macOS terminal for `run-project.sh`

Default local ports:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Backend health endpoint: `http://localhost:8080/api/health`

## 3. Quickest Way to Run the Project

Two helper launcher scripts are provided in the project root.

### Windows

Run:

```bat
run-project.bat
```

What it does:

- checks whether Java, Maven, Node.js, and npm are installed
- installs frontend dependencies automatically if `frontend/node_modules` is missing
- starts the Spring Boot backend in one terminal window
- starts the Vite frontend in another terminal window

How to stop:

- close the two launched terminal windows
- or press `Ctrl+C` inside each terminal

### Linux, macOS, Git Bash, or WSL

Run:

```bash
chmod +x run-project.sh
./run-project.sh
```

What it does:

- checks whether Java, Maven, Node.js, and npm are installed
- installs frontend dependencies automatically if needed
- starts backend and frontend together
- stops both processes when you press `Ctrl+C`

## 4. Normal Manual Run Method

This is the standard way developers usually run the project.

### Step 1: Start the backend

Open a terminal in the project root and run:

```bash
cd backend
mvn spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

### Step 2: Start the frontend

Open a second terminal in the project root and run:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on:

```text
http://localhost:3000
```

### Step 3: Open the application

Open this URL in your browser:

```text
http://localhost:3000
```

## 5. How Local Connection Works

During local development:

- Vite runs on port `3000`
- Spring Boot runs on port `8080`
- the frontend calls the backend through `/api`

Relevant configuration:

- `frontend/vite.config.js` proxies `/api` to `http://localhost:8080`
- `frontend/src/services/api.js` uses `VITE_API_BASE_URL` if provided, otherwise defaults to `http://localhost:8080/api`
- `backend/src/main/resources/application.properties` keeps the backend on port `8080`

## 6. First-Time Setup Notes

If you are running the project for the first time:

1. Install Java `17`.
2. Install Maven `3.9+`.
3. Install Node.js `18+`, which also gives you npm.
4. Open a fresh terminal after installation so the new commands are available in your PATH.
5. Verify the tools are installed correctly.
6. Install frontend dependencies.
7. Start the backend.
8. Start the frontend.

### 6.1 Install Java 17

You need Java `17` because the Spring Boot backend is built to run on that version.

Windows options:

- install Eclipse Temurin 17
- install Oracle JDK 17
- install Microsoft Build of OpenJDK 17

After installation, open a new terminal and run:

```bash
java -version
```

Expected result:

- the command should work without an error
- the version should show Java `17`

If `java` is not recognized:

- reopen the terminal
- restart your computer if needed
- confirm that Java was added to your system PATH

### 6.2 Install Maven

Maven is required to build and run the backend.

Windows installation methods:

- install Maven manually from the Apache Maven website and add its `bin` folder to PATH
- or use a package manager such as Chocolatey or Winget if you already use one

After installation, open a new terminal and run:

```bash
mvn -version
```

Expected result:

- Maven version information is displayed
- Java home is detected correctly
- the command runs without errors

If `mvn` is not recognized:

- verify Maven is installed
- verify Maven's `bin` folder is in PATH
- make sure Java is installed first because Maven depends on Java

### 6.3 Install Node.js and npm

The frontend requires Node.js and npm.

Recommended approach:

- install the LTS version of Node.js `18+`

After installation, run:

```bash
node -v
npm -v
```

Expected result:

- `node -v` shows version `18` or newer
- `npm -v` shows a valid npm version

If the commands fail:

- reopen the terminal
- check whether Node.js was added to PATH
- reinstall Node.js if needed

### 6.4 Verify the Project Folder

Before installing dependencies, make sure you are in the project root:

```bash
cd path/to/StegaCryptWeb_FIXED
```

You should be able to see folders such as:

- `backend`
- `frontend`
- `docs`

### 6.5 Install Frontend Dependencies

Move to the frontend folder and install the required packages:

```bash
cd frontend
npm install
```

What happens here:

- npm reads `package.json`
- npm downloads the frontend packages into `frontend/node_modules`
- npm creates or updates the lockfile if needed

If installation fails:

- check your internet connection
- check whether npm is installed correctly
- try running the command again after deleting a broken partial install only if needed

### 6.6 Start the Backend for the First Time

Open a terminal in the project root and run:

```bash
cd backend
mvn spring-boot:run
```

What happens here:

- Maven downloads backend dependencies the first time
- Spring Boot compiles and starts the API server
- the backend becomes available on port `8080`

Wait until the console shows that the application has started, then check:

```text
http://localhost:8080/api/health
```

If the backend does not start:

- check Java version
- check Maven installation
- check whether port `8080` is already in use

### 6.7 Start the Frontend for the First Time

Open a second terminal in the project root and run:

```bash
cd frontend
npm run dev
```

What happens here:

- Vite starts the development server
- the frontend becomes available on port `3000`
- the frontend begins sending API requests to the backend

Then open:

```text
http://localhost:3000
```

If the frontend does not start:

- check whether `npm install` completed successfully
- check whether port `3000` is free
- confirm that Node.js and npm are available in the terminal

### 6.8 Important First-Run Notes

- Maven may take a few minutes on the first run because it downloads backend dependencies.
- npm may take a few minutes on the first run because it downloads frontend dependencies.
- The backend must stay running because encryption, decryption, steganography, authentication, and secure chat logic all run server-side.
- The frontend depends on the backend, so opening the UI without the backend running will cause API errors.
- If you prefer a simpler startup workflow, use `run-project.bat` on Windows or `run-project.sh` on Bash-compatible terminals.

## 7. User Manual

The application has three main tabs:

- `Embed Message`
- `Extract Message`
- `Secure Chat`

### 7.1 Embed Message

Purpose:

- hide a secret message inside an image
- generate and download the key file required for extraction

Steps:

1. Open the `Embed Message` tab.
2. Upload a carrier image.
3. Type the secret message.
4. Click `Generate & Download`.
5. Save the downloaded key text file safely.
6. Keep compression enabled unless you specifically want to turn it off.
7. Click `Embed Message`.
8. Download the generated PNG stego image.

Important rules:

- keep the downloaded key file safe
- the key file contains the private key needed for extraction
- always use the generated PNG output for sharing or testing
- avoid editing, compressing, or resaving the stego image after embedding
- JPG or other lossy formats can damage the hidden payload

### 7.2 Extract Message Using Key File

Purpose:

- recover a message from a normal stego image created from the `Embed Message` tab

Steps:

1. Open the `Extract Message` tab.
2. Keep extraction mode on `Key File`.
3. Upload the stego image.
4. Upload the matching downloaded StegaCrypt key text file.
5. Click `Extract Message`.
6. Read or copy the extracted plaintext.

If extraction fails:

- verify that the uploaded key file matches the image
- verify that the image was not modified after embedding
- verify that the image is the PNG generated by StegaCrypt

### 7.3 Extract Message Using Secure Chat Members

Purpose:

- recover a message from a secure-chat stego image by choosing the correct sender and recipient accounts

Steps:

1. Open the `Extract Message` tab.
2. Switch extraction mode to `Secure Chat Members`.
3. Upload the secure-chat stego image.
4. Select the original sender.
5. Select the original recipient.
6. Click `Extract Message`.

Important rule:

- if sender or recipient is wrong, extraction will fail with a wrong-keys style error

### 7.4 Secure Chat Login and Registration

Purpose:

- send hidden stego messages between logged-in members inside the application

Options:

- log in using a built-in seeded member account
- register a new account from the UI

Built-in seeded accounts:

- `abhishek.sushant.chaskar`
- `aditya.atul.deshpande`
- `tanvi.dongare`
- `atharva.abhijeet.mahalkar`
- `prakhar.kumar.vishawakarma`

Default password for seeded accounts:

```text
Stega@123
```

Registration notes:

- full name is required
- username must be at least 4 characters
- password must be at least 6 characters

### 7.5 Secure Chat Message Flow

Steps:

1. Open the `Secure Chat` tab.
2. Log in as a seeded member or create a new account.
3. Choose a recipient.
4. Upload a carrier image.
5. Type the hidden message.
6. Click `Send Secure Chat Share`.
7. Download the generated PNG from the chat timeline if needed.
8. Log in as the recipient to decrypt the message from the chat timeline.

Important behavior:

- only the intended recipient can decrypt a secure-chat share
- secure chat data is stored in backend memory only
- if the backend restarts, registered runtime accounts, session tokens, and chat history are reset

## 8. Important Usage Notes

- The key file is essential for normal extraction.
- Do not lose the matching key file.
- Always keep the stego image as PNG.
- Do not edit the image after embedding.
- The hidden payload is tied to the correct key material.
- Secure-chat extraction depends on the correct recipient account and, when selected, the correct sender account.

## 9. API Summary

Base URL locally:

```text
http://localhost:8080/api
```

Main endpoints:

- `POST /generate-keys` - generate RSA key pair
- `POST /embed` - hide encrypted text inside an image
- `POST /extract` - extract text using the private key from the key file
- `POST /capacity` - check image capacity
- `GET /health` - backend health check
- `GET /demo-users` - demo user list
- `POST /auth/register` - create secure chat account
- `POST /auth/login` - log in to secure chat
- `GET /auth/chat` - load secure chat data using `X-Auth-Token`
- `GET /auth/members` - list secure chat members
- `POST /auth/chat/send` - send secure-chat stego image
- `POST /auth/chat/decrypt/{shareId}` - recipient decrypts a chat share
- `POST /auth/extract-shared-image` - recover secure-chat image by sender and recipient selection

## 10. Deployment Instructions

The project is designed for split deployment:

- frontend on Netlify
- backend on Render

### 10.1 Deploy Backend on Render

This repository already contains:

- `backend/Dockerfile`
- `render.yaml`

Backend deployment steps:

1. Push the repository to GitHub.
2. Open Render.
3. Create a new Web Service from the repository, or use the provided `render.yaml`.
4. Set the root directory to `backend` if you are configuring manually.
5. Ensure the health check path is:

```text
/api/health
```

6. Set the backend environment variable:

```text
FRONTEND_ORIGINS=http://localhost:3000,http://localhost:5173,https://YOUR_NETLIFY_SITE.netlify.app
```

7. Deploy the backend.
8. Copy the deployed backend URL.

Example:

```text
https://your-backend-name.onrender.com
```

Backend health check example:

```text
https://your-backend-name.onrender.com/api/health
```

### 10.2 Deploy Frontend on Netlify

This repository already contains:

- `netlify.toml`

Netlify build settings are already configured as:

```text
Base directory: frontend
Build command: npm run build
Publish directory: dist
```

Frontend deployment steps:

1. Open Netlify.
2. Import the GitHub repository.
3. Confirm the build settings from `netlify.toml`.
4. Add this environment variable:

```text
VITE_API_BASE_URL=https://YOUR_RENDER_BACKEND_URL/api
```

Example:

```text
VITE_API_BASE_URL=https://your-backend-name.onrender.com/api
```

5. Deploy the frontend.
6. Copy the Netlify site URL.

### 10.3 Final CORS Update

After Netlify gives you the final frontend URL:

1. go back to Render
2. update `FRONTEND_ORIGINS`
3. add the final Netlify site URL explicitly
4. redeploy the backend

Example:

```text
FRONTEND_ORIGINS=http://localhost:3000,http://localhost:5173,https://your-site-name.netlify.app
```

## 11. Production and Hosting Notes

- Render free instances may take time to wake up after inactivity.
- Netlify hosts only the built frontend files.
- Render runs the Java backend and processes cryptography and image operations.
- The frontend and backend are intentionally deployed separately because they have different runtime needs.

## 12. Build Commands

### Frontend development

```bash
cd frontend
npm run dev
```

### Frontend production build

```bash
cd frontend
npm install
npm run build
```

### Frontend preview

```bash
cd frontend
npm run preview
```

### Backend development

```bash
cd backend
mvn spring-boot:run
```

### Backend package build

```bash
cd backend
mvn clean package
```

## 13. Troubleshooting

### Frontend does not start

Check:

- Node.js and npm are installed
- `npm install` completed successfully
- port `3000` is free

### Backend does not start

Check:

- Java 17 is installed
- Maven is installed
- port `8080` is free

### Frontend loads but API calls fail

Check:

- backend is running
- backend health endpoint responds at `http://localhost:8080/api/health`
- `VITE_API_BASE_URL` is correct in deployment
- CORS origins are correct on Render

### Extraction fails

Check:

- correct key file was used
- correct sender and recipient were selected for secure chat extraction
- image was not modified after embedding
- the file is the PNG produced by StegaCrypt

### Secure chat data disappears after restart

This is expected because:

- secure chat users, sessions, and shares are stored in memory
- restarting the backend clears runtime data

## 14. Code Directory Structure

```text
StegaCryptWeb_FIXED/
|-- backend/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/main/
|       |-- java/com/stegacrypt/
|       |   |-- StegaCryptApplication.java
|       |   |-- controller/
|       |   |   |-- AuthChatController.java
|       |   |   `-- SteganographyController.java
|       |   |-- exception/
|       |   |   `-- GlobalExceptionHandler.java
|       |   |-- model/
|       |   |   |-- EmbedRequest.java
|       |   |   |-- EmbedResult.java
|       |   |   |-- ExtractRequest.java
|       |   |   |-- ExtractResult.java
|       |   |   `-- StegaResponse.java
|       |   |-- service/
|       |   |   |-- AuthChatService.java
|       |   |   |-- CompressionService.java
|       |   |   |-- DemoUserService.java
|       |   |   |-- ImageProcessingService.java
|       |   |   `-- SteganographyService.java
|       |   `-- util/
|       |       |-- AESUtil.java
|       |       |-- BitUtil.java
|       |       |-- PRNGUtil.java
|       |       |-- RSAUtil.java
|       |       `-- ValidationUtil.java
|       `-- resources/
|           `-- application.properties
|-- frontend/
|   |-- package.json
|   |-- package-lock.json
|   |-- vite.config.js
|   `-- src/
|       |-- App.css
|       |-- App.jsx
|       |-- main.jsx
|       |-- components/
|       |   |-- EmbedSection.jsx
|       |   |-- ExtractSection.jsx
|       |   |-- ImageUpload.jsx
|       |   `-- ShareDemoSection.jsx
|       |-- services/
|       |   `-- api.js
|       `-- utils/
|           `-- keyFile.js
|-- docs/
|   |-- API_DOCS.md
|   |-- DEPLOYMENT.md
|   |-- HOSTING_AND_PROJECT_WORKING.md
|   |-- PROJECT_REPORT.md
|   |-- QUICKSTART.md
|   `-- RUN_ME.md
|-- README.md
|-- netlify.toml
|-- render.yaml
|-- run-project.bat
|-- run-project.sh
`-- test-artifacts/
```

## 15. Role of Important Files

- `frontend/src/App.jsx` controls the main UI tabs
- `frontend/src/components/EmbedSection.jsx` handles message embedding
- `frontend/src/components/ExtractSection.jsx` handles normal extraction and secure-chat member extraction
- `frontend/src/components/ShareDemoSection.jsx` handles secure chat login, registration, send, download, and decrypt flows
- `frontend/src/services/api.js` manages frontend-to-backend API requests
- `frontend/src/utils/keyFile.js` creates and reads StegaCrypt key text files
- `backend/src/main/java/com/stegacrypt/controller/SteganographyController.java` exposes normal steganography APIs
- `backend/src/main/java/com/stegacrypt/controller/AuthChatController.java` exposes authentication and secure chat APIs
- `backend/src/main/java/com/stegacrypt/service/AuthChatService.java` manages in-memory users, tokens, and chat shares
- `backend/src/main/java/com/stegacrypt/service/SteganographyService.java` embeds and extracts encrypted payload data
- `backend/src/main/java/com/stegacrypt/service/ImageProcessingService.java` loads, validates, and saves images
- `backend/src/main/java/com/stegacrypt/util/AESUtil.java` handles AES encryption and decryption
- `backend/src/main/java/com/stegacrypt/util/RSAUtil.java` handles RSA key generation, parsing, and wrapping operations
- `backend/src/main/resources/application.properties` stores backend runtime configuration
- `render.yaml` stores backend Render deployment configuration
- `netlify.toml` stores frontend Netlify deployment configuration

## 16. Final Notes

If you want the easiest workflow:

1. run `run-project.bat` on Windows or `run-project.sh` on Bash/Linux-like terminals
2. open `http://localhost:3000`
3. generate a key file before embedding
4. keep the key file and stego PNG safe
5. use the correct key file or correct secure-chat users during extraction

This file is intended to be the single reference document for project running, user operation, deployment, and structure.
