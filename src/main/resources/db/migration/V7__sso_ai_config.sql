-- =============================================================================
-- DnXT Global Admin Portal — SSO & AI Configuration
-- Seeds SSO config from previously hardcoded Google values,
-- and adds AI feature configuration.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- SSO Configuration (seeded from hardcoded Google Workspace SSO values)
-- ---------------------------------------------------------------------------
INSERT INTO platform_config (config_key, config_value, category, description, is_secret) VALUES
('sso.enabled',              'true',               'SSO', 'Enable Single Sign-On authentication',                     FALSE),
('sso.provider',             'google',              'SSO', 'SSO provider (google, azure_ad, okta, saml)',              FALSE),
('sso.google.client_id',     '',                    'SSO', 'Google OAuth2 Client ID',                                  FALSE),
('sso.google.client_secret', '',                    'SSO', 'Google OAuth2 Client Secret',                              TRUE),
('sso.google.redirect_uri',  'http://localhost:8110/api/auth/google/callback', 'SSO', 'Google OAuth2 Redirect URI',   FALSE),
('sso.google.hosted_domain', 'dnxtsolutions.com',   'SSO', 'Google Workspace hosted domain restriction (hd parameter)', FALSE),
('sso.google.scopes',        'openid email profile','SSO', 'Google OAuth2 scopes',                                     FALSE),
('sso.google.prompt',        'consent',             'SSO', 'Google OAuth2 prompt type (consent, select_account, none)', FALSE),
('sso.password_fallback',    'true',                'SSO', 'Allow emergency password login when SSO is enabled',        FALSE),
('sso.auto_create_users',    'false',               'SSO', 'Auto-create users on first SSO login (invite-only if false)', FALSE);

-- ---------------------------------------------------------------------------
-- AI Configuration
-- ---------------------------------------------------------------------------
INSERT INTO platform_config (config_key, config_value, category, description, is_secret) VALUES
('ai.enabled',                          'true',      'AI', 'Enable AI features platform-wide',                          FALSE),
('ai.provider',                         'anthropic', 'AI', 'AI provider (anthropic, openai, azure_openai, ollama)',      FALSE),
('ai.api_key',                          '',          'AI', 'AI provider API key',                                        TRUE),
('ai.model',                            'claude-sonnet-4-20250514', 'AI', 'Default AI model name',                       FALSE),
('ai.gateway_url',                      '',          'AI', 'AI Gateway service URL (internal)',                           FALSE),
('ai.max_tokens',                       '4096',      'AI', 'Maximum tokens per AI request',                              FALSE),
('ai.temperature',                      '0.3',       'AI', 'AI response temperature (0.0 = deterministic, 1.0 = creative)', FALSE),
('ai.features.document_classification', 'true',      'AI', 'Enable AI-powered document classification in EDMS',         FALSE),
('ai.features.smart_search',            'true',      'AI', 'Enable AI-powered semantic search across modules',           FALSE),
('ai.features.content_generation',      'false',     'AI', 'Enable AI content generation and suggestions',               FALSE),
('ai.features.document_summarization',  'false',     'AI', 'Enable AI document summarization',                           FALSE),
('ai.features.compliance_check',        'false',     'AI', 'Enable AI compliance and quality checks',                    FALSE),
('ai.rate_limit.requests_per_minute',   '60',        'AI', 'Max AI requests per minute per tenant',                      FALSE),
('ai.rate_limit.tokens_per_day',        '1000000',   'AI', 'Max AI tokens per day per tenant',                           FALSE);
