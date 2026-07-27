-- ============================================================
-- PostgreSQL - Database creation if not already exists
-- Database: invoices
-- Execute with psql:
--   psql -d postgres -f 01_create_database.sql
-- ============================================================


SELECT 'CREATE DATABASE invoices'
    WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'invoices'
)\gexec