-- Keep the 39 imported Excel rules in their original relative order while
-- making the GAB interface rule visible immediately in the unfiltered list.
UPDATE monitoring_rules
SET source_order = 0
WHERE code = 'RULE_ATM_INTERFACE_UNAVAILABLE_5X'
  AND source_order IS NULL;
