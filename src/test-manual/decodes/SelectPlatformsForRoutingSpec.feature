@Kiwi.Plan(DbEdit)
Feature: Routing Spec
  
    Background: Basic Setup
        Given A database with many platforms in available. Routing Spec can exist, or be created.

    Scenario: Adding Individual Platforms to a Routing Spec
        Given The user clicks "Select Platform"
          And The user sorts the rows by any column
          And The user clicks select
         Then The correct platform is added to the "Platforms Selection" List.

    Scenario: Adding Platform by PDT
        Given The user clicks "Select from PDT"
          And The user sorts the rows by any column
          And The user select and row
         When The user clicks Okay
         Then The correct platform is added to the "Platforms Selection" List.