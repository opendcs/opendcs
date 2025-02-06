@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Routing Spec
  
    Background: Basic Setup
        Given A database with many platforms is available. 
        And A Routing Spec has been opened or created.

    Scenario: Adding Individual Platforms to a Routing Spec
        Given The user clicks "Select Platform"
          And The user sorts the rows by any column
          And The user selects a row and clicks Select
         Then The correct platform is added to the "Platform Selection" List.

    Scenario: Adding Platform by PDT
        Given The user clicks "Select from PDT"
          And The user sorts the rows by any column
          And The user select and row
         When The user clicks Okay
         Then The correct platform is added to the "Platform Selection" List.