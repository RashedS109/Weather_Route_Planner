# Weather-Adaptive Road Trip Planner 🌦️🚗

**WeatherRoutePlanner** is a native Android application built in Java that dynamically calculates the safest and most efficient driving routes between cities. It integrates live weather data to intelligently avoid hazardous conditions by recalculating road "weights" using graph algorithms.

This project was developed as a submission for the **Advanced Programming Laboratory** course, demonstrating core Java concepts, Android architecture, and algorithmic implementation.

## 🚀 Key Features & Syllabus Mapping

This project is structured to fulfill specific weekly learning objectives:

*   **Java OOP & Syntax (Week 1):** Utilizes standard Java Object-Oriented Programming principles to model the graph network (`City` nodes and `Road` edges).
*   **Version Control (Week 2):** Maintained using Git with iterative commits and pushed to a remote GitHub repository.
*   **Multithreading & Concurrency (Week 4):** Uses Java `ExecutorService` to offload heavy network operations and API parsing to background threads, ensuring the UI remains responsive, with `runOnUiThread` handling view updates.
*   **SQLite Database Integration (Week 6):** Features a robust local SQLite database with table creation, insert, and query operations to cache city coordinate data.
*   **JSON Parsing & APIs (Week 7 & 11):** Connects to the public OpenWeatherMap REST API via `HttpURLConnection`, fetching and parsing JSON data natively in Java to read current weather conditions.
*   **Android Architecture & UI (Week 8 & 9):** Built with modern Android XML layouts, View Binding, and `RecyclerViews` with custom ViewHolders (`item_route_step.xml`) to dynamically display step-by-step navigation.

## 🛠️ Tech Stack

*   **Language:** Java
*   **Environment:** Android Studio Rabbit 1 (2026.2.1)
*   **Minimum SDK:** API 26 (Android 8.0)
*   **Database:** SQLite
*   **External API:** OpenWeatherMap API

## ⚙️ Setup & Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YourUsername/WeatherRoutePlanner.git
   ```
2. **Open in Android Studio:**
   Select `File > Open` and choose the cloned directory.
3. **Add API Key:**
   * Open `WeatherService.java`.
   * Replace the placeholder `"YOUR_API_KEY_HERE"` with a free API key from [OpenWeatherMap](https://openweathermap.org/api).
4. **Build and Run:**
   Connect an Android device (via USB Debugging) or start an emulator, and press the **Run** (Green Play) button.

## 🗺️ Upcoming Features (Phase 6)

*   **Dijkstra's Algorithm Implementation:** The core routing logic is currently being finalized. Once implemented, the app will construct an adjacency matrix using the local SQLite database and dynamically shift road weights based on the live weather fetched from the API, calculating the absolute shortest/safest path to the destination.

---
*Developed for Advanced Programming Lab.*