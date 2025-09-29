# Contributing to Subscription Licensing System

Thank you for your interest in contributing to this project! This document provides guidelines and instructions for contributing.

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of background or identity.

### Our Standards

**Positive behavior includes:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behavior includes:**
- Harassment, trolling, or discriminatory comments
- Personal or political attacks
- Publishing others' private information without permission
- Other conduct which could reasonably be considered inappropriate

## How to Contribute

### Reporting Bugs

Before creating a bug report, please check existing issues to avoid duplicates.

**When filing a bug report, include:**
- A clear and descriptive title
- Detailed steps to reproduce the issue
- Expected behavior vs. actual behavior
- Platform/environment details (OS, PHP version, etc.)
- Relevant logs or error messages
- Screenshots if applicable

### Suggesting Enhancements

Enhancement suggestions are welcome! Please provide:
- A clear and descriptive title
- Detailed description of the proposed functionality
- Use cases and benefits
- Potential implementation approach (optional)
- Any relevant examples from other projects

### Security Vulnerabilities

**DO NOT** open public issues for security vulnerabilities. Instead, email details to:
- **Security Contact**: alan@whitemail.net

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Development Process

### Getting Started

1. **Fork the repository**
   ```bash
   git clone https://github.com/yourusername/sub-lic-spec.git
   cd sub-lic-spec
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow the coding standards below
   - Add tests for new functionality
   - Update documentation as needed

4. **Commit your changes**
   ```bash
   git commit -m "Brief description of changes"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Open a Pull Request**

### Pull Request Guidelines

**Before submitting:**
- Ensure code follows project coding standards
- All tests pass
- Documentation is updated
- Commit messages are clear and descriptive

**PR Description should include:**
- Summary of changes
- Motivation and context
- Related issue numbers (if applicable)
- Testing performed
- Screenshots (for UI changes)

### Commit Message Guidelines

Use clear, concise commit messages:

```
[Component] Brief description

Detailed explanation of what changed and why.

Fixes #123
```

**Examples:**
```
[Server] Add certificate revocation endpoint

Implements CRL-based revocation for account deletion.
Includes database migration and API endpoint.

Fixes #45
```

```
[Client] Improve offline license validation

Adds grace period handling and better error messages
for expired subscriptions.
```

## Coding Standards

### PHP (Server)

- Follow PSR-12 coding standard
- Use meaningful variable and function names
- Add PHPDoc comments for classes and methods
- Keep functions focused and under 50 lines when possible
- Use type hints for parameters and return values

**Example:**
```php
/**
 * Validates an enrollment token and returns user details
 * 
 * @param string $token The enrollment token to validate
 * @return array User and subscription details
 * @throws InvalidTokenException If token is invalid or expired
 */
public function validateToken(string $token): array {
    // Implementation
}
```

### Java (Client)

- Follow standard Java conventions
- Use meaningful class and variable names
- Add Javadoc comments for public methods
- Keep methods focused and under 50 lines
- Use proper exception handling

### Flutter/Dart (Client)

- Follow Dart style guide
- Use meaningful widget and variable names
- Add documentation comments for public APIs
- Keep widgets focused and composable
- Use proper error handling

### Database

- Use descriptive table and column names
- Add indexes for frequently queried columns
- Include comments for complex queries
- Use transactions for multi-step operations
- Follow the existing schema patterns

### Documentation

- Update spec.md for architectural changes
- Keep README.md current with features
- Add inline code comments for complex logic
- Include examples for new APIs
- Use clear, concise language

## Testing

### Server Tests

```bash
# Run PHP unit tests
composer test

# Run integration tests
composer test:integration
```

### Client Tests

```bash
# Java tests
mvn test

# Flutter tests
flutter test
```

### Test Coverage

- Aim for >80% code coverage for new code
- Include unit tests for business logic
- Include integration tests for API endpoints
- Test error conditions and edge cases

## Project Structure

```
sub-lic-spec/
├── spec.md              # Complete technical specification
├── README.md            # Project overview
├── LICENSE              # MIT License
├── CONTRIBUTING.md      # This file
├── server/              # PHP server implementation
│   ├── api/            # API endpoints
│   ├── services/       # Business logic
│   └── tests/          # Server tests
├── client/              # Client implementations
│   ├── flutter/        # Flutter/Dart client
│   ├── java/           # Java client
│   └── swift/          # Swift client
└── schema/              # Database schemas
```

## Code Review Process

All contributions go through code review:

1. **Automated checks** run on PR submission
2. **Maintainer review** for code quality and architecture
3. **Feedback addressed** by contributor
4. **Approval and merge** by maintainer

**Reviewers check for:**
- Code quality and readability
- Adherence to project standards
- Test coverage
- Documentation completeness
- Security implications
- Performance considerations

## Areas for Contribution

**Good first issues:**
- Documentation improvements
- Code comments and examples
- Test coverage improvements
- Bug fixes

**Advanced contributions:**
- New platform support (Android, iOS native)
- Performance optimizations
- Security enhancements
- Feature implementations from roadmap

## Questions?

If you have questions about contributing:
- Check existing issues and discussions
- Review the [spec.md](spec.md) documentation
- Email: alan@whitemail.net

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to the Subscription Licensing System!