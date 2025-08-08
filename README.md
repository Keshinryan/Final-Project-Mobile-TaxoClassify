# Animal Taxonomy Classification App (Final Project)

This mobile application is the final project titled **"Design of Animal Classification System Using Deep Learning on Android"**. It is designed to help users—especially elementary school students—identify animals and understand their taxonomy levels through image classification.

## Features

- **Image Classification**: Detects animals and classifies them into five taxonomy levels: `Kelas`, `Ordo`, `Famili`, `Genus`, and `Spesies`.
- **Offline & Online Inference**:
  - **Edge AI (Offline)** using TensorFlow Lite model.
  - **Cloud AI** via FastAPI and Hugging Face Spaces for online prediction.
- **Camera & Gallery Input**: Users can take a photo or select from gallery.
- **Compressed Image Upload**: Images are resized and compressed before being sent to the cloud model.
- **Educational Output**: Displays structured taxonomy and conservation message.

## Technology Stack

- **Frontend**: Android (Kotlin, Jetpack Compose)
- **Machine Learning**: TensorFlow (CNN Multi-output, Multi-class)
- **Deployment**:
  - TensorFlow Lite (Edge AI)
  - FastAPI on Hugging Face (Cloud AI)
  - Gradio UI + Vercel.js API integration (Cloud Alternative)

## Model Information

- Model Type: CNN (Sequential)
- Input Size: 256x256 (Edge) / 384x384 (Cloud)
- Classes: 7 Animal Species (Domestic Cat, Leopard Cat, Eurassian Tree Sparrow, House Sparrow, Rock Dove, Koi, Common Carp)
- Taxonomy Levels: Kelas, Ordo, Famili, Genus, Spesies
- Best Model: 4 Hidden Layers + Dropout 0.2 (Avg Accuracy: 90.15%)

## Installation

1. Clone this repository.
2. Open in Android Studio.
3. Ensure you have internet access for cloud inference.
4. Connect a device or emulator and run the app.

## Folder Structure

- `app/`: Main Android application
- `model/`: TensorFlow Lite model
- `cloud/`: API endpoints and deployment files for cloud inference

## Acknowledgements

This project was developed as a Final Project to promote biodiversity education and conservation awareness.

## 🔗 Related GitHub Repositories

This Mobile Application is integrated into different platforms through separate repositories:
- **Model Training**  
  [Final-Project-Model-Training](https://github.com/Keshinryan/Final-Project-Model-Training)
  
- **Cloud Deployment (Gradio + Vercel + Express.js)**  
  [Final-Project-Cloud-AI-Gradio-VercelJs-ExpressJs](https://github.com/Keshinryan/Final-Project-Cloud-AI-Gradio-VercelJs-ExpressJs)

- **Cloud Deployment (FastAPI)**  
  [Final-Project-Cloud-AI-FastAPI](https://github.com/Keshinryan/Final-Project-Cloud-AI-FastAPI)

## 🖋 License

This project is licensed under the MIT License.
