# RestAssured Framework

This project is a RestAssured framework built using Maven, TestNG, and Hamcrest matchers for testing RESTful APIs. It includes Allure reporting for enhanced test reporting and visualization.

## Project Structure

```
restassured-framework
├── src
│   ├── main
│   │   └── java
│   │       └── utils
│   │           └── ApiClient.java
│   └── test
│       └── java
│           ├── tests
│           │   └── RepositoryApiTest.java
│           └── testng.xml
├── pom.xml
└── README.md
```

## Setup Instructions

1. **Clone the repository:**
   ```
   git clone <repository-url>
   cd restassured-framework
   ```

2. **Build the project:**
   Ensure you have Maven installed, then run:
   ```
   mvn clean install
   ```

3. **Run the tests:**
   You can execute the tests using the following command:
   ```
   mvn test
   ```

4. **Generate Allure reports:**
   After running the tests, generate the Allure report with:
   ```
   mvn allure:report
   ```

5. **View Allure reports:**
   Open the generated report in your browser:
   ```
   mvn allure:serve
   ```

## Usage Examples

- The `ApiClient` class provides methods for making HTTP requests. You can use it to interact with various endpoints of the API.
- The `RepositoryApiTest` class contains test methods that cover different endpoints of the repository API, ensuring comprehensive testing.

## Additional Information

- Ensure you have the necessary dependencies specified in the `pom.xml` file.
- For detailed information on each test case, refer to the `RepositoryApiTest.java` file.

This framework is designed to be easily extendable for additional API testing needs.# GitHub-RestAssured

## Running Test
- Before running tests setup your environment variable by passing the github token 
- export GITHUB_TOKEN=Here_Paste_Your_Token

