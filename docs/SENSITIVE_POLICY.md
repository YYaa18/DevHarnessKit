# Sensitive Policy

DevHarness Kit rejects known sensitive patterns by default before persistence and export. That is safer for a public default, but it can be too strict in financial systems where emails, phone numbers, and identity numbers are common business data.

Project-level policy lives at:

```text
.agents/devharness/sensitive-policy.json
```

`dhk memory init` creates the `.agents/devharness/` directory. Create the JSON file manually when a project needs custom handling.

## Actions

Each key maps a sensitive pattern name to one action:

| Action | Meaning |
| --- | --- |
| `reject` | Block persistence/export when the pattern is present. This is the default for every pattern. |
| `redact` | Allow the command, but replace matches with a fixed placeholder before storing or writing output. |
| `allow` | Allow the raw value. Use only for data that is safe for agents and local exports. |

Recommended financial-system baseline:

```json
{
  "email": "redact",
  "phone": "redact",
  "identity_number": "redact",
  "jwt": "reject",
  "api_key": "reject",
  "private_key": "reject",
  "jdbc:mysql://": "reject",
  "private key block": "reject",
  "authorization:": "reject",
  "github token": "reject",
  "github pat": "reject",
  "url credential": "reject"
}
```

With that policy, `test@example.com`, `13800138000`, and `11010519491231002X` are stored/exported as:

```text
[REDACTED_EMAIL]
[REDACTED_PHONE]
[REDACTED_IDENTITY_NUMBER]
```

Credential-like values still reject by default. For example:

| Example shape | Recommended action | Reason |
| --- | --- | --- |
| `test@example.com` | `redact` or `allow` | Common business PII in requirements, tickets, and test cases. |
| `13800138000` | `redact` or `allow` | Common business PII in financial workflows. |
| `11010519491231002X` | `redact` or `allow` | Common business PII and high-risk if exported raw. |
| `jwt eyJ...` | `reject` | Authentication credential. |
| `api_key=...` | `reject` | Application credential. |
| `private_key=...` | `reject` | Key material should not enter project context. |
| `jdbc:mysql://...` | `reject` | May expose host, schema, user, or connection options. |
| `Authorization: ...` | `reject` | Request credential. |
| `ghp_...` / `github_pat_...` | `reject` | GitHub access token. |
| `https://user:password@...` | `reject` | URL-embedded credentials. |
| `-----BEGIN OPENSSH PRIVATE KEY-----` | `reject` | Private key material. |

## Supported Keys

The current pattern keys are:

```text
password=
passwd=
secret=
token=
api_key=
private_key=
accessKey=
secretKey=
jdbc:mysql://
authorization:
bearer credential
private key block
aws access key
github token
github pat
jwt
url credential
aliyun access key
google api key
slack token
cookie:
email
phone
identity number
```

Aliases are accepted for common names such as `identity_number`, `id_card`, `mobile`, `phone_number`, `api_key`, and `private_key`.

## Financial Project Examples

Use this when PII should never be written raw to memory or context exports:

```json
{
  "email": "redact",
  "phone": "redact",
  "identity_number": "redact",
  "jwt": "reject",
  "api_key": "reject",
  "private_key": "reject",
  "jdbc:mysql://": "reject",
  "private key block": "reject",
  "authorization:": "reject",
  "github token": "reject",
  "github pat": "reject",
  "url credential": "reject"
}
```

Use this only when local development data is synthetic or otherwise safe for agents to see:

```json
{
  "email": "allow",
  "phone": "allow",
  "identity_number": "allow",
  "jwt": "reject",
  "api_key": "reject",
  "private_key": "reject",
  "jdbc:mysql://": "reject",
  "private key block": "reject",
  "authorization:": "reject",
  "github token": "reject",
  "github pat": "reject",
  "url credential": "reject"
}
```

Use this for public examples or repositories where personal data should hard-stop persistence/export:

```json
{
  "email": "reject",
  "phone": "reject",
  "identity_number": "reject",
  "jwt": "reject",
  "api_key": "reject",
  "private_key": "reject",
  "jdbc:mysql://": "reject",
  "private key block": "reject",
  "authorization:": "reject",
  "github token": "reject",
  "github pat": "reject",
  "url credential": "reject"
}
```

## Guidance

Prefer `redact` for personal data that is common in business requirements, test cases, and database inspection results.

Keep credentials and secrets at the default `reject` action:

```json
{
  "email": "redact",
  "phone": "redact",
  "identity_number": "redact",
  "password=": "reject",
  "token=": "reject",
  "api_key": "reject",
  "private_key": "reject",
  "jdbc:mysql://": "reject",
  "private key block": "reject"
}
```

The policy is intentionally simple: a JSON object with string keys and string values. Invalid policy files are ignored and the default reject behavior is used.

The guard is still heuristic and is not a DLP system. Review generated Markdown before sharing it outside the local project environment.
