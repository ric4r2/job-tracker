-- Demo data for the "local" profile only. Never runs against PostgreSQL in production.
insert into job_applications (company, role, location, job_url, source, notes, status, applied_on, next_follow_up_on, created_at, updated_at)
values
    ('Acme Payments', 'Junior Java Developer', 'Remote', 'https://example.com/jobs/1', 'LinkedIn', 'Spring Boot + Kafka team', 'INTERVIEWING', current_date - 12, current_date - 1, current_timestamp, current_timestamp),
    ('Northwind Logistics', 'Backend Engineer I', 'Guadalajara', 'https://example.com/jobs/2', 'Company site', null, 'APPLIED', current_date - 5, current_date + 2, current_timestamp, current_timestamp),
    ('Globex', 'Software Engineer, New Grad', 'Hybrid', 'https://example.com/jobs/3', 'Referral', 'Ask Ana about the team', 'WISHLIST', null, null, current_timestamp, current_timestamp),
    ('Initech', 'Java Developer', 'Remote', 'https://example.com/jobs/4', 'Indeed', null, 'REJECTED', current_date - 30, null, current_timestamp, current_timestamp);

insert into status_changes (job_application_id, from_status, to_status, note, changed_at)
select id, null, 'APPLIED', 'Created', current_timestamp from job_applications where company <> 'Globex';
insert into status_changes (job_application_id, from_status, to_status, note, changed_at)
select id, null, 'WISHLIST', 'Created', current_timestamp from job_applications where company = 'Globex';
insert into status_changes (job_application_id, from_status, to_status, note, changed_at)
select id, 'APPLIED', 'INTERVIEWING', 'Recruiter call booked', current_timestamp from job_applications where company = 'Acme Payments';
insert into status_changes (job_application_id, from_status, to_status, note, changed_at)
select id, 'APPLIED', 'REJECTED', 'Generic rejection email', current_timestamp from job_applications where company = 'Initech';
