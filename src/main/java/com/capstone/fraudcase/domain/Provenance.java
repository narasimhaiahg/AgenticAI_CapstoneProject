package com.capstone.fraudcase.domain;

/** Where a fact came from. Every evidence item, citation and decision carries one. */
public enum Provenance { USER_INTAKE, TOOL_RESULT, POLICY_RETRIEVAL, PROCESSOR_SUMMARY, REVIEWER_ENTRY, SYSTEM }
