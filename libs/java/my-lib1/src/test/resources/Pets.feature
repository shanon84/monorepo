Feature: Pets
  As a user
  I want to receive pets by id
  So that I get an existing one

  Scenario: Retrieving a pet by ID
    Given a pet with ID "123"
    When I retrieve the pet
    Then the pet details should be returned
