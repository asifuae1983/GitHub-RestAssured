package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import listeners.RetryAnalyzer;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pojo.RepositoryTestData;
import utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Listeners({io.qameta.allure.testng.AllureTestNg.class, listeners.TestResultListener.class})
public class CheckRepoContentTest {

    private RepositoryTestData testData;
    private RequestSpecification requestSpec;

    // Utility method for colored output: yellow for pass, red for fail
    private void printStatus(String msg, boolean isPass) {
        String color = isPass ? "\u001B[33m" : "\u001B[31m";
        System.out.println(color + msg + "\u001B[0m");
    }

    @BeforeClass
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        testData = mapper.readValue(new File("src/test/resources/testdata/TestData.json"), RepositoryTestData.class);
        RestAssured.baseURI = Config.getBaseUri();
        requestSpec = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json");
    }

    /**
     * Test to check the content of a file or directory in the repository.
     * Fetches the content metadata for a given path and verifies the response.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Content")
    @Story("Get Repository Content")
    @Description("Checks if the content metadata for a given path in the repository can be fetched successfully.")
    @Test(priority = 1, retryAnalyzer = RetryAnalyzer.class)
    public void testGetRepositoryContent() {
        String path = "README.md";
        try {
            Response response = given()
                .spec(requestSpec)
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/contents/" + path)
            .then()
                .statusCode(200)
                .body("name", equalTo(path))
                .body("type", anyOf(equalTo("file"), equalTo("dir")))
                .extract().response();

            String msg = "Test passed: testGetRepositoryContent. Fetched content metadata for path: " + path + " in repo: " + testData.getRepo();
            printStatus(msg, true);
            Allure.step(msg);

            // Display decoded content of README.md
            String readmeContentEncoded = response.jsonPath().getString("content");
            String readmeEncoding = response.jsonPath().getString("encoding");
            if (readmeContentEncoded != null && "base64".equalsIgnoreCase(readmeEncoding)) {
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(readmeContentEncoded.replaceAll("\\s", ""));
                String readmeContentDecoded = new String(decodedBytes);
                System.out.println("\u001B[32mContent of README.md:\n" + readmeContentDecoded + "\u001B[0m");
                Allure.step("Displayed content of README.md file.");
            } else {
                System.out.println("README.md content not available or not base64 encoded.");
            }

        } catch (Exception e) {
            String msg = "Test failed: testGetRepositoryContent";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testGetRepositoryContent: " + e.getMessage());
            Assert.fail("Exception in testGetRepositoryContent: " + e.getMessage(), e);
        }
    }    

    /**
     * Test to create a new file (test-file.txt) in the repository using the GitHub API.
     * This test will fail if the file already exists.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Content")
    @Story("Create File Content")
    @Description("Creates a new file in the repository using the GitHub API. Fails if the file already exists.")
    @Test(priority = 2, retryAnalyzer = RetryAnalyzer.class)
    public void testCreateFileContent() {
        String path = "test-file.txt";
        String content = java.util.Base64.getEncoder().encodeToString("This is a test file created by API.".getBytes());
        String commitMessage = "Create test-file.txt via API";
        try {
            // Prepare request body using a Map for better maintainability
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", commitMessage);
            requestBody.put("content", content);

            given()
                .spec(requestSpec)
                .header("Content-Type", "application/json")
                .body(requestBody) // RestAssured will serialize the map to JSON
            .when()
                .put("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/contents/" + path)
            .then()
                .statusCode(201) // 201 Created, fails if file exists
                .body("content.name", equalTo(path))
                .extract().response();

            String msg = "Test passed: testCreateFileContent. File '" + path + "' created successfully in repo: " + testData.getRepo();
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testCreateFileContent";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testCreateFileContent: " + e.getMessage());
            Assert.fail("Exception in testCreateFileContent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test to delete the file (test-file.txt) created by testCreateFileContent using the GitHub API.
     * This test fetches the file's SHA and then deletes it.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Content")
    @Story("Delete File Content")
    @Description("Deletes the file test-file.txt in the repository using the GitHub API.")
    @Test(priority = 3, dependsOnMethods = "testCreateFileContent", retryAnalyzer = RetryAnalyzer.class)
    public void testDeleteFileContent() {
        String path = "test-file.txt";
        String commitMessage = "Delete test-file.txt via API";
        try {
            // Step 1: Get the file's SHA
            Response getResponse = 
            given()
                .spec(requestSpec)
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/contents/" + path)
            .then()
                .statusCode(200)
                .extract().response();

            String sha = getResponse.jsonPath().getString("sha");

            // Step 2: Prepare delete request body using a Map for better maintainability
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", commitMessage);
            requestBody.put("sha", sha);

            // Step 3: Delete the file
            given()
                .spec(requestSpec)
                .header("Content-Type", "application/json")
                .body(requestBody) // RestAssured will serialize the map to JSON
            .when()
                .delete("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/contents/" + path)
            .then()
                .statusCode(200);

            String msg = "Test passed: testDeleteFileContent. File '" + path + "' deleted successfully from repo: " + testData.getRepo();
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testDeleteFileContent";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testDeleteFileContent: " + e.getMessage());
            Assert.fail("Exception in testDeleteFileContent: " + e.getMessage(), e);
        }
    }
}