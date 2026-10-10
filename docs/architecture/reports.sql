-- Income/expense report parameters: user UUID, from DATE, to DATE, currency, user timezone.
SELECT
  coalesce(sum(amount_minor) FILTER (WHERE kind='income'), 0)::text AS income_minor,
  coalesce(sum(amount_minor) FILTER (WHERE kind='expense'), 0)::text AS expense_minor
FROM transactions
WHERE user_id=$1 AND deleted_at IS NULL AND currency=$4
  AND occurred_on BETWEEN $2 AND least($3::date, (now() AT TIME ZONE $5)::date);

-- Category report, including budgets with zero expenses.
-- Same parameters; use the user's salary-period start/end from the server.
WITH spending AS (
  SELECT category_id, sum(amount_minor) AS used
  FROM transactions
  WHERE user_id=$1 AND deleted_at IS NULL AND kind='expense' AND currency=$4
    AND occurred_on BETWEEN $2 AND least($3::date, (now() AT TIME ZONE $5)::date)
  GROUP BY category_id
)
SELECT c.id, c.name, coalesce(s.used,0)::text AS used_minor,
  b.limit_minor::text,
  CASE WHEN b.id IS NULL THEN NULL ELSE (b.limit_minor-coalesce(s.used,0))::text END AS remaining_minor,
  CASE WHEN b.id IS NULL THEN 'no_budget'
       WHEN coalesce(s.used,0)>b.limit_minor THEN 'exceeded'
       WHEN coalesce(s.used,0)*100>=b.limit_minor*80 THEN 'near_limit'
       ELSE 'within_limit' END AS status
FROM categories c
LEFT JOIN spending s ON s.category_id=c.id
LEFT JOIN budgets b ON b.category_id=c.id AND b.user_id=$1 AND b.currency=$4
  AND b.period_start=$2 AND b.period_end=$3
WHERE c.user_id=$1 AND c.kind='expense' AND (c.archived_at IS NULL OR s.used IS NOT NULL OR b.id IS NOT NULL)
ORDER BY coalesce(s.used,0) DESC,c.id;
