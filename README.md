# README.md

## System Name
**MyAlkansya**

## Product Description
**MyAlkansya** is a financial management application designed to help users track income, expenses, and budgeting. It provides features such as **data visualization**, **multiple login options**, **currency conversion**, and **export capabilities** to enhance financial planning. The application is available on both **web and mobile platforms**.

---

## List of Features
- **User Authentication**
  - Email & Password Login
  - Social Media Login (Google, Facebook)
  - Biometric Login (Mobile)
- **Dashboard**
  - Overview of financial health
  - Total income, total expenses, remaining budget
- **Expense Tracking**
  - Categorization of expenses (e.g., Food, Transportation)
  - Edit, delete, and add new transactions
- **Income Tracking**
  - Record and manage income sources
  - Categorization based on source
- **Budgeting**
  - Set monthly budgets
  - Alerts for overspending
- **Currency Converter**
  - Supports multiple currencies
  - Integrates with ExchangeRate-API
- **Data Analytics & Visualization**
  - Charts and insights using Chart.js
  - Monthly spending reports
- **Savings Goals**
  - Set and track savings objectives
- **Export to Google Sheets**
  - Automatic data export using Google Sheets API
- **Data Insights & Tips**
  - Personalized recommendations based on spending patterns

---

## Setup Instructions

### Prerequisites
Ensure you have the following installed on your system:
- **Node.js** (v16 or later)
- **npm** (v7 or later)
- **Java** (JDK 11 or later)
- **Maven** (for backend)
- **MySQL** (or any compatible database)
- **Android Studio** (for mobile development)
- **Git** (for version control)

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend/myalkansya
   ```
2. Install Maven dependencies:
   ```bash
   mvn clean install
   ```
3. Configure database connection in `src/main/resources/application.properties`
4. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   The backend API will be available at `http://localhost:8080`

### Frontend Web Setup
1. Navigate to the frontend web directory:
   ```bash
   cd frontend_web/myalkansya-app
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm start
   ```
   The web application will be available at `http://localhost:3000`

### Frontend Mobile Setup
1. Open Android Studio
2. Open the project from `frontend_mobile/myAlkansyaMob`
3. Sync Gradle files and install dependencies
4. Connect an Android device or start an emulator
5. Build and run the application

## Usage Guide

### Getting Started
1. Register a new account or log in with existing credentials
2. Complete your profile setup to personalize the experience

### Managing Finances
1. **Adding Income**:
   - Navigate to the Income section
   - Click on "Add Income"
   - Fill in the required details (amount, source, date)
   - Save your entry

2. **Tracking Expenses**:
   - Go to the Expenses section
   - Click on "Add Expense"
   - Select a category, enter the amount and date
   - Add optional notes for reference

3. **Setting Budgets**:
   - Access the Budget section
   - Set monthly limits for different expense categories
   - The system will alert you when approaching budget limits

### Using Analytics
1. View your spending patterns through visual charts on the Dashboard
2. Generate monthly/yearly reports to analyze your financial habits
3. Export data to Google Sheets for further analysis

### Managing Savings Goals
1. Create a new savings goal with target amount and deadline
2. Track progress and make contributions
3. Receive notifications upon reaching milestones

## Troubleshooting

### Common Issues
- **Database Connection Errors**: Ensure MySQL is running and credentials are correct
- **Login Problems**: Clear browser cache or verify account credentials
- **Mobile App Crashes**: Check for app updates or reinstall the application

### Getting Support
For technical assistance, contact the development team through GitHub issues or email.

## Links
- [Figma Design](#) 'To be added'
- [System Diagrams](#) 'To be added'

## Developer Profiles

### Member1
### NAME: Christian Hans Israel E. Paras
- **Course & Year:** BSIT - 3

### Member2
### NAME: Aeron Raye Tigley
- **Course & Year:** BSIT - 3

### Member1
### NAME: Christian Hans Israel E. Paras
- **Course & Year:** BSIT - 3

### Member2
### NAME: Aeron Raye Tigley
- **Course & Year:** BSIT - 3

### Member3
### NAME: Craig Matthew Cartilla
- **Course & Year:** BSIT - 3

## About Me (Team Member 1)

Hello, my name is **Christian Hans Israel E. Paras**, a third-year **Bachelor of Science in Information Technology** student at **Cebu Institute of Technology - University**. I am highly motivated, opportunistic, and determined to excel in my field, always seeking ways to grow and expand my knowledge.

### Passion for Cybersecurity
My ultimate goal is to become a **Cybersecurity Specialist**. I am deeply fascinated by the complexities of digital security and ethical hacking, always eager to learn new techniques to protect and strengthen networks against cyber threats. I actively explore various IT domains, including:

- **Cybersecurity & Ethical Hacking**
- **Network Security**
- **Penetration Testing**
- **Software Development**
- **Cloud Computing**

### Determination & Growth
I believe that success is achieved through consistent effort and the right mindset. I seize every opportunity that allows me to improve, whether through hands-on projects, self-learning, or collaboration with like-minded individuals. I thrive in an environment where innovation and continuous learning are encouraged.

## Let's Connect!
I am always open to networking, collaboration, and discussions related to IT, cybersecurity, and technology in general. Feel free to reach out if you share the same interests or would like to work together!


## About Me (Team Member 2)

Hello, my name is **Aeron Raye Tigley**, a third-year BSIT student from **Cebu Institute of Technology - University**. I am passionate about technology and aspire to build a career in **cybersecurity**, where I can contribute to protecting digital systems and data. Outside of academics, I am a chill person who enjoys spending time at home, relaxing, and exploring my interests. I also have a deep love for **swimming and free diving**, often taking the opportunity to visit the ocean whenever I can.

### Future Goal
My future goal is to become a skilled cybersecurity professional, working to safeguard information and systems from cyber threats. I am dedicated to continuous learning and growth in this ever-evolving field.

### Swimming and Free Diving
I am passionate about swimming in the ocean and free diving, as it allows me to connect with nature and unwind from the demands of daily life. 
Things I Visited:
- **Moalboal Sardines Run** 
- **Bantayan** 
- **Palawan** 

### Let's Connect
Feel free to reach out if you want to connect or collaborate!

## About Me (Team Member 3)

Hello, my name is **Craig Matthew Cartilla**, a third-year BSIT student from **Cebu Institute of Technology - University**. 

I am an outgoing person who enjoys going to parties every weekend. During the weekdays, I engage in strenuous physical activities, including:

- Jogging
- Working out
- Boxing
- Swimming
- Hiking

However, in the coming months, I will likely be very busy with school activities and projects.

### About My Dog
I have a dog named **Birkin**, a Labrador and Husky mix, who is currently 5 years old. I love taking her for walks in the afternoon; she really enjoys it!

### Love for Travel
I am passionate about traveling. Whenever I get the chance, I visit:

- Mountains
- Beaches
- Other interesting and memorable places

### Goals and Ambitions
I have many ambitions and goals I want to achieve in life. This drives me to be successful with every action I take and to surround myself with people from whom I can grow and learn.

---

Feel free to reach out if you want to connect or collaborate with this Project!

