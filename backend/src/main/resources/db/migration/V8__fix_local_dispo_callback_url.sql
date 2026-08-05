UPDATE company
SET callback_url = 'http://localhost:8090/api/dispo/confirmation-status-updates'
WHERE callback_url = 'http://localhost:8081/api/dispo/callback';
