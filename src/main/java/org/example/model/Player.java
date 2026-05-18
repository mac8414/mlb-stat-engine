package org.example.model;

public class Player {
    private Integer id;
    private String fullName;
    private String currentTeam;
    private String primaryPosition;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCurrentTeam() { return currentTeam; }
    public void setCurrentTeam(String currentTeam) { this.currentTeam = currentTeam; }

    public String getPrimaryPosition() { return primaryPosition; }
    public void setPrimaryPosition(String primaryPosition) { this.primaryPosition = primaryPosition; }
}
