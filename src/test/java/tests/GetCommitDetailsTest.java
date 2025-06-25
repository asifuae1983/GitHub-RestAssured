package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import listeners.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pojo.RepositoryTestData;
import utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Listeners({io.qameta.allure.testng.AllureTestNg.class, listeners.TestResultListener.class})
public class GetCommitDetailsTest {

    private RepositoryTestData testData;

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
    }

    /**
     * Test to list all commits in the repository.
     * Fetches the list of commits and prints/logs their SHAs.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Commits")
    @Story("List Commits")
    @Description("Lists all commits in the repository using the GitHub API.")
    @Test(priority = 1, retryAnalyzer = RetryAnalyzer.class)
    public void testListCommits() {
        try {
            Response response = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/commits")
            .then()
                .statusCode(200)
                .body("sha", notNullValue())
                .extract().response();

            List<String> commitShas = response.jsonPath().getList("sha");
            String msg;
            if (commitShas == null || commitShas.isEmpty()) {
                msg = "Test failed: No commits found in the repository.";
                printStatus(msg, false);
                Allure.step(msg);
                Assert.fail(msg);
            } else {
                msg = "Test passed: testListCommits. Commits: " + commitShas;
                printStatus(msg, true);
                Allure.step(msg);
            }
        } catch (Exception e) {
            String msg = "Test failed: testListCommits";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListCommits: " + e.getMessage());
            assert false : "Exception in testListCommits: " + e.getMessage();
        }
    }
    
    /**
     * Test to get a specific commit by ref (SHA, branch, or tag).
     * Fetches commit details and validates the response.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Commits")
    @Story("Get Commit by Ref")
    @Description("Gets a specific commit by ref (SHA, branch, or tag) using the GitHub API.")
    @Test(priority = 2, retryAnalyzer = RetryAnalyzer.class)
    public void testGetCommitByRef() {
        String ref = "master"; // You can replace with a specific commit SHA or branch/tag name
        try {
            Response response = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/commits/" + ref)
            .then()
                .statusCode(200)
                .body("sha", notNullValue())
                .body("commit.message", notNullValue())
                .extract().response();

            String commitSha = response.jsonPath().getString("sha");
            String msg = "Test passed: testGetCommitByRef. Commit SHA: " + commitSha;
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testGetCommitByRef";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testGetCommitByRef: " + e.getMessage());
            assert false : "Exception in testGetCommitByRef: " + e.getMessage();
        }
    }
    
    /**
     * Test to compare two commits using the endpoint /repos/{owner}/{repo}/compare/{basehead}.
     * Compares the master branch with the develop branch (or any two refs).
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Commits")
    @Story("Compare Commits")
    @Description("Compares two commits or branches using the GitHub API.")
    @Test(priority = 3, retryAnalyzer = RetryAnalyzer.class)
    public void testCompareCommits() {
        String base = "master";   // You can change this to any base branch or commit SHA
        String head = "develop";  // You can change this to any head branch or commit SHA
        String basehead = base + "..." + head;
        try {
            Response response = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/compare/" + basehead)
            .then()
                .statusCode(200)
                .body("status", notNullValue())
                .body("commits", notNullValue())
                .extract().response();

            String status = response.jsonPath().getString("status");
            int totalCommits = response.jsonPath().getList("commits").size();
            String msg = String.format("Test passed: testCompareCommits. Status: %s, Total commits compared: %d", status, totalCommits);
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testCompareCommits";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testCompareCommits: " + e.getMessage());
            assert false : "Exception in testCompareCommits: " + e.getMessage();
        }
    }
}
