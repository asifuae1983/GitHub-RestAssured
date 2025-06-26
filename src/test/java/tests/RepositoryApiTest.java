package tests;

import api.GitHubRepositoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import listeners.RetryAnalyzer;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import utils.Config;
import utils.ConsoleUtils;
import pojo.RepositoryTestData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Listeners({io.qameta.allure.testng.AllureTestNg.class, listeners.TestResultListener.class})
public class RepositoryApiTest {

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec200;
    private RepositoryTestData testData;
    private GitHubRepositoryClient repoClient;
    private String createdRepoName;

    // Utility method for colored output: yellow for pass, red for fail
    private void printStatus(String msg, boolean isPass) {
        String color = isPass ? "\u001B[33m" : "\u001B[31m";
        System.out.println(color + msg + "\u001B[0m");
    }

    // WE ARE TRYING TO AUTOMATE APIs PRESENT IN https://docs.github.com/en/rest/repos?apiVersion=2022-11-28

    @BeforeClass
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        testData = mapper.readValue(new File("src/test/resources/testdata/TestData.json"), RepositoryTestData.class);

        RestAssured.baseURI = Config.getBaseUri();
        requestSpec = RestAssured.given()
            .basePath("/repos/{owner}/{repo}")
            .pathParam("owner", testData.getOwner())
            .pathParam("repo", testData.getRepo())
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + Config.getAuthToken());

        responseSpec200 = RestAssured.expect().statusCode(200);

        repoClient = new GitHubRepositoryClient(requestSpec);
    }

    /**
     * Test to fetch repository details and validate the full_name and private status.
     * Verifies that the repository exists and matches the expected owner/repo and privacy setting.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Info")
    @Story("Get Repository Details")
    @Description("Checks if the repository details can be fetched successfully.")
    @Test(priority = 1, retryAnalyzer = RetryAnalyzer.class)
    public void testGetRepository() {
        try {
            given()
                .spec(requestSpec)
            .when()
                .get()
            .then()
                .log().ifError()
                .spec(responseSpec200)
                .body("full_name", equalTo(testData.getOwner() + "/" + testData.getRepo()))
                .body("private", equalTo(testData.isPrivateRepo()))
                .extract().response();

            String msg = "Test passed: testGetRepository";
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testGetRepository";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testGetRepository: " + e.getMessage());
            org.testng.Assert.fail("Exception in testGetRepository: " + e.getMessage());
        }
    }

    /**
     * Test to update the repository description.
     * Sends a PATCH request to update the description and verifies the change in the response.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Management")
    @Story("Update Repository Description")
    @Description("This test updates the repository description and verifies the change.")
    @Test(priority = 2, retryAnalyzer = RetryAnalyzer.class)
    public void testUpdateRepository() {
        try {
            Map<String, Object> updateBody = new HashMap<>();
            updateBody.put("name", testData.getRepo());
            updateBody.put("description", testData.getUpdateDescription());

            given()
                .spec(requestSpec)
                .body(updateBody)
            .when()
                .patch()
            .then()
                .statusCode(200)
                .body("description", equalTo(testData.getUpdateDescription()));
            String msg = "Test passed: testUpdateRepository";
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testUpdateRepository";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testUpdateRepository: " + e.getMessage());
            assert false : "Exception in testUpdateRepository: " + e.getMessage();
        }
    }

    /**
     * Test to list repository activities/events.
     * Handles both the case where there are events and where the event list is empty (both are valid).
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Activities")
    @Story("List Repository Events")
    @Description("Checks if repository activities/events can be listed. Handles empty event list as valid.")
    @Test(priority = 3, retryAnalyzer = RetryAnalyzer.class)
    public void testListRepositoryActivities() {
        try {
            Response response = given()
                .spec(requestSpec)
            .when()
                .get("/events")
            .then()
                .statusCode(200)
                .extract().response();

            String msg;
            if (response.jsonPath().getList("$").isEmpty()) {
                msg = "Test passed: testListRepositoryActivities (no activities/events found, empty array is valid)";
            } else {
                msg = "Test passed: testListRepositoryActivities (activities/events found)";
            }
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testListRepositoryActivities";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListRepositoryActivities: " + e.getMessage());
            assert false : "Exception in testListRepositoryActivities: " + e.getMessage();
        }
    }

    /**
     * Test to delete the repository.
     * Sends a DELETE request and expects a 204 status code. Disabled by default for safety.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Management")
    @Story("Delete Repository")
    @Description("Deletes the repository. Disabled by default.")
    @Test(priority = 4, enabled = false, retryAnalyzer = RetryAnalyzer.class)
    public void testDeleteRepository() {
        try {
            given()
                .spec(requestSpec)
            .when()
                .delete()
            .then()
                .statusCode(204);
            String msg = "Test passed: testDeleteRepository";
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testDeleteRepository";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testDeleteRepository: " + e.getMessage());
            assert false : "Exception in testDeleteRepository: " + e.getMessage();
        }
    }

    /**
     * Test to check if Dependabot security updates are enabled for the repository.
     * Handles all possible status codes and logs the result.
     */
    @Epic("GitHub Repository API")
    @Feature("Dependabot Security")
    @Story("Check Dependabot Security Updates")
    @Description("Checks if Dependabot security updates are enabled for the repository.")
    @Test(priority = 5, retryAnalyzer = RetryAnalyzer.class)
    public void testCheckDependabotSecurityUpdatesEnabled() {
        try {
            Response response = 
            given()
                .spec(requestSpec)
            .when()
                .get("/automated-security-fixes")
            .then()
                .statusCode(anyOf(is(200), is(204), is(404)))
                .extract().response();

            String msg;
            if (response.statusCode() == 204) {
                msg = "Test passed: testCheckDependabotSecurityUpdatesEnabled (ENABLED)";
            } else if (response.statusCode() == 404) {
                msg = "Test passed: testCheckDependabotSecurityUpdatesEnabled (NOT ENABLED)";
            } else if (response.statusCode() == 200) {
                msg = "Test passed: testCheckDependabotSecurityUpdatesEnabled (200 response)";
            } else {
                msg = "Test passed: testCheckDependabotSecurityUpdatesEnabled (Unexpected status: " + response.statusCode() + ")";
            }
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testCheckDependabotSecurityUpdatesEnabled";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testCheckDependabotSecurityUpdatesEnabled: " + e.getMessage());
            assert false : "Exception in testCheckDependabotSecurityUpdatesEnabled: " + e.getMessage();
        }
    }

    /**
     * Test to list contributors for the repository.
     * Fetches the contributors and prints/logs their usernames, or a message if none are found.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Contributors")
    @Story("List Repository Contributors")
    @Description("Checks if the list of contributors for the repository can be fetched successfully.")
    @Test(priority = 6, retryAnalyzer = RetryAnalyzer.class)
    public void testListRepositoryContributors() {
        try {
            Response response = 
            given()
                .spec(requestSpec)
            .when()
                .get("/contributors")
            .then()
                .statusCode(200)
                .extract().response();

            List<String> repoNames = response.jsonPath().getList("login");
            String msg;
            if (repoNames == null || repoNames.isEmpty()) {
                msg = "Test passed: testListRepositoryContributors (no contributors found)";
            } else {
                msg = "Test passed: testListRepositoryContributors. Contributors: " + repoNames;
            }
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testListRepositoryContributors";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListRepositoryContributors: " + e.getMessage());
            assert false : "Exception in testListRepositoryContributors: " + e.getMessage();
        }
    }

    /**
     * Test to fetch all public repositories (first page).
     * Fetches the list of public repositories, prints/logs the count and names.
     */
    @Epic("GitHub Repository API")
    @Feature("Public Repositories")
    @Story("List All Public Repositories")
    @Description("Checks if the list of all public repositories can be fetched successfully.")
    @Test(priority = 7, retryAnalyzer = RetryAnalyzer.class)
    public void testListAllPublicRepositories() {
        try {
            Response response = 
            given()
                .baseUri(Config.getBaseUri()) // Use config-driven base URI
                .header("Authorization", "Bearer " + Config.getAuthToken())
            .when()
                .get("/repositories")
            .then()
                .statusCode(200)
                .extract().response();

            List<String> repoNames = response.jsonPath().getList("name");
            String msg;
            if (repoNames == null || repoNames.isEmpty()) {
                msg = "Test passed: testListAllPublicRepositories (no public repositories found)";
            } else {
                msg = "Test passed: testListAllPublicRepositories. Count: " + repoNames.size() + ". Names: " + repoNames;
            }
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testListAllPublicRepositories";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListAllPublicRepositories: " + e.getMessage());
            assert false : "Exception in testListAllPublicRepositories: " + e.getMessage();
        }
    }

    /**
     * Test to fetch public repositories using pagination (first 4 pages).
     * Loops through pages 1 to 4, fetches repositories for each page, and prints/logs the total count and names.
     */
    @Epic("GitHub Repository API")
    @Feature("Public Repositories")
    @Story("List All Public Repositories With Pagination")
    @Description("Fetches repositories from the first 4 pages using pagination.")
    @Test(priority = 8, retryAnalyzer = RetryAnalyzer.class)
    public void testListAllPublicRepositoriesWithPagination() {
        try {
            List<String> allRepoNames = new ArrayList<>();
            int perPage = 30; // GitHub default, can be set up to 100

            for (int page = 1; page <= 4; page++) {
                Response response = 
                given()
                    .baseUri(Config.getBaseUri()) // Use config-driven base URI
                    .header("Authorization", "Bearer " + Config.getAuthToken())
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                .when()
                    .get("/repositories")
                .then()
                    .statusCode(200)
                    .extract().response();

                List<String> repoNames = response.jsonPath().getList("name");
                if (repoNames != null && !repoNames.isEmpty()) {
                    allRepoNames.addAll(repoNames);
                }
                String msg = "Test passed: testListAllPublicRepositoriesWithPagination (Page " + page + ": " + (repoNames != null ? repoNames.size() : 0) + " repositories)";
                printStatus(msg, true);
                Allure.step(msg);
            }

            String summaryMsg = "Test passed: testListAllPublicRepositoriesWithPagination. Total repositories fetched from 4 pages: " + allRepoNames.size() + ". Names: " + allRepoNames;
            printStatus(summaryMsg, true);
            Allure.step(summaryMsg);

        } catch (Exception e) {
            String msg = "Test failed: testListAllPublicRepositoriesWithPagination";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListAllPublicRepositoriesWithPagination: " + e.getMessage());
            assert false : "Exception in testListAllPublicRepositoriesWithPagination: " + e.getMessage();
        }
    }

    /**
     * Test to fetch repositories accessible by the authenticated user.
     * Uses the /user/repos endpoint, requires authentication.
     * Prints/logs the count and names of repositories, or a message if none are found.
     */
    @Epic("GitHub Repository API")
    @Feature("User Repositories")
    @Story("List Authenticated User's Repositories")
    @Description("Checks if the list of repositories accessible by the authenticated user can be fetched successfully.")
    @Test(priority = 9, retryAnalyzer = RetryAnalyzer.class)
    public void testListAuthenticatedUserRepositories() {
        try {
            RequestSpecification userRequestSpec = RestAssured.given()
                .baseUri(Config.getBaseUri())
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + Config.getAuthToken());

            Response response = 
            given()
                .spec(userRequestSpec)
                .log().ifValidationFails() // Log request/response if validation fails
            .when()
                .get("/user/repos")
            .then()
                .statusCode(200)
                .extract().response();

            List<String> repoNames = response.jsonPath().getList("name");
            String msg;
            if (repoNames == null || repoNames.isEmpty()) {
                msg = "Test passed: testListAuthenticatedUserRepositories (no repositories found for the authenticated user)";
            } else {
                msg = "Test passed: testListAuthenticatedUserRepositories. Count: " + repoNames.size() + ". Names: " + repoNames;
            }
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testListAuthenticatedUserRepositories";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListAuthenticatedUserRepositories: " + e.getMessage());
            assert false : "Exception in testListAuthenticatedUserRepositories: " + e.getMessage();
        }
    }

    /**
     * Test to create a new repository for the authenticated user.
     * Stores the created repository name for deletion in a later test.
     */
    @Epic("GitHub Repository API")
    @Feature("User Repositories")
    @Story("Create Repository for Authenticated User")
    @Description("Creates a new repository for the authenticated user and verifies the creation.")
    @Test(priority = 10, retryAnalyzer = RetryAnalyzer.class)
    public void testCreateRepositoryForAuthenticatedUser() {
        createdRepoName = "test-repo-" + System.currentTimeMillis();
        try {
            String requestBody = "{ \"name\": \"" + createdRepoName + "\", \"description\": \"Repository created via API test\", \"private\": false }";
            given()
                .baseUri(Config.getBaseUri()) // Use config-driven base URI
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .body(requestBody)
            .when()
                .post("/user/repos")
            .then()
                .statusCode(201)
                .body("name", equalTo(createdRepoName))
                .extract().response();

            String msg = "Test passed: testCreateRepositoryForAuthenticatedUser. Repository: " + createdRepoName;
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testCreateRepositoryForAuthenticatedUser";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testCreateRepositoryForAuthenticatedUser: " + e.getMessage());
            assert false : "Exception in testCreateRepositoryForAuthenticatedUser: " + e.getMessage();
        }
    }

    /**
     * Test to delete the repository created in testCreateRepositoryForAuthenticatedUser.
     * Uses the stored repository name and sends a DELETE request.
     */
    @Epic("GitHub Repository API")
    @Feature("User Repositories")
    @Story("Delete Repository for Authenticated User")
    @Description("Deletes the repository created by the authenticated user in the previous test.")
    @Test(priority = 11, dependsOnMethods = "testCreateRepositoryForAuthenticatedUser", retryAnalyzer = RetryAnalyzer.class)
    public void testDeleteRepositoryForAuthenticatedUser() {
        try {
            if (createdRepoName == null) {
                String msg = "Test failed: testDeleteRepositoryForAuthenticatedUser (No repository name stored from creation test. Skipping delete.)";
                printStatus(msg, false);
                Allure.step(msg);
                assert false : msg;
            }
            Response response = 
            given()
                .baseUri(Config.getBaseUri()) // Use config-driven base URI
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .delete("/repos/" + testData.getOwner() + "/" + createdRepoName)
            .then()
                .statusCode(204)
                .extract().response();

            String msg = "Test passed: testDeleteRepositoryForAuthenticatedUser. Repository: " + createdRepoName;
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testDeleteRepositoryForAuthenticatedUser";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testDeleteRepositoryForAuthenticatedUser: " + e.getMessage());
            assert false : "Exception in testDeleteRepositoryForAuthenticatedUser: " + e.getMessage();
        }
    }
}
