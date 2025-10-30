@Kiwi.Plan(DbEdit)
@Kiwi.Priority(Medium)
Feature: Decodes Script Trace Dialog

    Background: Basic Setup
        Given A decodes script that can be run.
    
    Scenario: With Tracing open, logs are shown

        Given The user has navigated to creating or editing a decodes script
        When The user clicks the "Trace" button
        Then The TraceDialog is opened

        When The user cliks the "Decode" button
        Then Log messages are shown in the TraceDialog
  
