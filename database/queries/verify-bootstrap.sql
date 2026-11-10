SELECT environment, schema_version FROM bootstrap.app_metadata ORDER BY environment;
SELECT version, description FROM bootstrap.schema_history ORDER BY version;
SELECT count(*) AS retained_verification_markers FROM bootstrap.verification_marker;
