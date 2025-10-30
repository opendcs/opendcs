@Kiwi.Plan(CompEdit)
@Kiwi.Priority(Medium)
Feature: Comp Run Trace Dialog

    Background: Basic Setup
        Given A Time Series database with a computation and data available to run.
        Given The user has navigated to running a computation
    
    Scenario: With Tracing open, logs are shown

        When The user clicks the "Trace Execution" button
        Then The TraceDialog is opened

        When The user clicks the "Execute" button
        Then Log messages are shown in the TraceDialog

    Scenario: With Auto Scroll

        Given A long computation
        When The AutoScroll button is enabled
        And  The user clicks Execute
        Then The list of messages is kept at the bottom so new messages are visible.

    Scenario: Without Auto Scroll
  
        Given A long computation
        When The AutoScroll button is disable
        And  The user clicks Execute
        Then The list of messages is kept in place