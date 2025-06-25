package pojo;

public class RepositoryTestData {
    private String owner;
    private String repo;
    private boolean privateRepo;
    private String updateDescription;
    private String attestationBody;
    private String subjectDigest;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public boolean isPrivateRepo() {
        return privateRepo;
    }

    public void setPrivateRepo(boolean privateRepo) {
        this.privateRepo = privateRepo;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public void setUpdateDescription(String updateDescription) {
        this.updateDescription = updateDescription;
    }

    public String getAttestationBody() {
        return attestationBody;
    }

    public void setAttestationBody(String attestationBody) {
        this.attestationBody = attestationBody;
    }

    public String getSubjectDigest() {
        return subjectDigest;
    }

    public void setSubjectDigest(String subjectDigest) {
        this.subjectDigest = subjectDigest;
    }
}