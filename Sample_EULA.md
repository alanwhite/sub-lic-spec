# END USER LICENSE AGREEMENT

**Last Updated: [Date]**

**IMPORTANT – READ CAREFULLY:** This End User License Agreement ("Agreement") is a legal agreement between you (either an individual or a single entity, "You" or "Licensee") and [Your Company Name] ("Company," "We," "Us," or "Our") for the software product identified above, which includes computer software and may include associated media, printed materials, and online or electronic documentation (collectively, the "Software").

**BY INSTALLING, COPYING, OR OTHERWISE USING THE SOFTWARE, YOU AGREE TO BE BOUND BY THE TERMS OF THIS AGREEMENT. IF YOU DO NOT AGREE TO THE TERMS OF THIS AGREEMENT, DO NOT INSTALL OR USE THE SOFTWARE.**

---

## 1. GRANT OF LICENSE

### 1.1 Free Tier License
We grant You a limited, non-exclusive, non-transferable, revocable license to install and use the free tier of the Software ("Free Tier") without charge for your personal or internal business purposes. The Free Tier includes basic functionality and does not require account creation or subscription.

### 1.2 Premium Tier License (Subscription-Based)
Subject to your compliance with this Agreement and payment of applicable subscription fees, We grant You a limited, non-exclusive, non-transferable, revocable license to install and use the premium features of the Software ("Premium Tier") on a single device during the active subscription period.

### 1.3 Premium Tier Requirements
Access to Premium Tier features requires:
- Account creation and enrollment
- Certificate-based authentication (issued by Our private Certificate Authority)
- Maintaining an active, paid subscription
- Compliance with all terms of this Agreement
- Successful license validation as described in Section 3

### 1.4 Single Device License (Premium Tier Only)
Each Premium Tier license is bound to a single device. You may migrate your Premium license to a different device using the migration feature provided in the Software, subject to the limitations described in Section 2.4.

---

## 2. LICENSE RESTRICTIONS

### 2.1 Prohibited Actions (All Tiers)
You may NOT:
- Copy, modify, or create derivative works of the Software
- Reverse engineer, decompile, or disassemble the Software
- Rent, lease, lend, sell, redistribute, or sublicense the Software
- Remove or alter any proprietary notices or labels on the Software
- Use the Software for any unlawful purpose

### 2.2 Additional Premium Tier Restrictions
If using Premium Tier features, You may NOT:
- Attempt to circumvent or disable the license validation mechanism
- Share your license credentials, certificates, or license tokens with others
- Use the Premium features on more than one device simultaneously
- Extract or attempt to extract license validation keys or certificates for unauthorized use
- Downgrade to Free Tier to avoid payment while retaining Premium feature data

### 2.3 No Unbundling
The Software is licensed as a single product. Its component parts may not be separated for use on more than one device.

### 2.4 License Migration (Premium Tier Only)
You may migrate your Premium license to a new device up to [NUMBER] times per [TIME PERIOD] using the built-in migration feature. Each migration permanently deactivates the license on the previous device. Abuse of the migration feature may result in license suspension.

---

## 3. NETWORK CONNECTIVITY AND LICENSE VALIDATION

### 3.1 Free Tier - No Network Requirement
**The Free Tier operates entirely offline and does NOT require:**
- Account creation
- Network connectivity to Our servers
- Certificate authentication
- License validation

The Free Tier does not transmit any information to Our servers during normal operation.

### 3.2 Premium Tier - Certificate-Based Authentication
**Premium Tier features use digital certificates issued by Our private Certificate Authority to verify your subscription.** Upon subscribing to Premium features, you will receive a client certificate that uniquely identifies your licensed installation.

### 3.3 Premium Tier - Network Connections Required
**IMPORTANT: Premium Tier features require periodic network connections to Our license servers for the following purposes:**

a) **Initial Activation**: When first subscribing to Premium, the Software will connect to Our servers to:
   - Validate your enrollment token
   - Obtain your client certificate
   - Receive your initial license token

b) **License Renewal**: The Software will automatically connect to Our servers:
   - Approximately [7] days before your subscription expires
   - To verify your subscription payment status
   - To renew your license token if payment is current
   - At intervals during any grace period

c) **Certificate Operations**: The Software may connect to Our servers to:
   - Renew expiring certificates (approximately 30 days before expiration)
   - Check certificate revocation status
   - Validate license integrity

d) **License Migration**: When transferring your Premium license to a new device, the Software will connect to Our servers to:
   - Validate the migration request
   - Deactivate the license on the old device
   - Activate the license on the new device

### 3.4 Offline Operation (Premium Tier)
Premium Tier features are designed to operate offline for the duration of your active subscription period. However, periodic network connectivity is required for license renewal and certificate management. Extended offline use beyond your subscription expiration date will result in loss of Premium functionality (reverting to Free Tier).

### 3.5 Grace Period (Premium Tier)
If your subscription payment fails or expires, Premium features will continue to function for a limited grace period:
- **Monthly subscriptions**: [5] days
- **Annual subscriptions**: [14] days

During the grace period, the Software will attempt to connect to Our servers daily to check for subscription renewal. After the grace period expires without payment, Premium features will be disabled and Free Tier features will remain available.

### 3.6 Network Requirements (Premium Tier Only)
Premium Tier features require:
- HTTPS connectivity to [license-server.yourdomain.com] on ports 443 and 8443
- Ability to validate SSL/TLS certificates
- No proxy authentication (or proper proxy configuration)

### 3.7 Firewall Considerations (Premium Tier Only)
If you use Premium features with a firewall, you must allow outbound HTTPS connections to Our license servers. Blocking these connections will prevent Premium license validation and may result in loss of Premium functionality.

---

## 4. DATA COLLECTION AND PRIVACY

### 4.1 Free Tier - No Data Collection
**The Free Tier does NOT collect, transmit, or store any personal information or usage data on Our servers.** All Free Tier operations occur locally on your device.

### 4.2 Premium Tier - Information Collected
When using Premium Tier features, the Software transmits the following information to Our servers during license validation:
- Your client certificate (contains your name and organization as provided during enrollment)
- Device identifier (hardware-based unique ID)
- License status and expiration dates
- IP address (collected automatically by Our servers)
- Software version number

### 4.3 Purpose of Collection (Premium Tier)
We collect this information solely to:
- Verify your subscription status
- Prevent unauthorized license sharing
- Manage license activations and migrations
- Provide customer support
- Comply with legal obligations

### 4.4 Data Storage and Security
All data transmitted between the Software and Our servers is encrypted using TLS. License data is stored securely on Our servers and is not shared with third parties except as required by law or as described in Our Privacy Policy.

### 4.5 No Usage Tracking (All Tiers)
We do NOT collect or transmit:
- Your usage patterns or behavior within the Software
- Files or content you create or modify
- Personal information beyond what is necessary for Premium license validation
- Analytics or telemetry data
- Any information from Free Tier users

### 4.6 Privacy Policy
Our complete Privacy Policy is available at [URL]. By using the Software, you agree to the data practices described in Our Privacy Policy.

---

## 5. CERTIFICATE AND KEY STORAGE (PREMIUM TIER ONLY)

### 5.1 Local Storage
When using Premium Tier features, the Software stores the following on your device:
- Your client certificate and private key (in macOS Keychain)
- Encrypted license tokens (in application support directory)
- Premium feature configuration files

Free Tier users do not have certificates or license tokens stored on their devices.

### 5.2 Security Responsibility (Premium Tier)
If you subscribe to Premium features, you are responsible for:
- Maintaining the security of your device and operating system
- Protecting your certificate and license credentials
- Not sharing your certificate or private key with others
- Promptly reporting any suspected compromise to Us

### 5.3 Certificate Ownership (Premium Tier)
The client certificates remain Our property and are licensed to You for the sole purpose of validating your Premium subscription. Upon termination of your subscription, certificates may be revoked, and Premium features will revert to Free Tier functionality.

---

## 6. SUBSCRIPTION TERMS (PREMIUM TIER)

### 6.1 Free Tier - No Subscription Required
Free Tier features are available without subscription, payment, or account creation. You may use Free Tier features indefinitely at no cost.

### 6.2 Premium Tier - Subscription Fees
Use of Premium features requires an active paid subscription. Subscription fees are billed:
- **Monthly**: Every 30 days
- **Annual**: Every 365 days

### 6.3 Payment (Premium Tier)
Payment is due in advance. Failure to pay subscription fees will result in:
- Inability to renew your Premium license token
- Grace period activation
- Eventual loss of Premium features (reversion to Free Tier)

### 6.4 Automatic Renewal (Premium Tier)
Premium subscriptions automatically renew unless you cancel before the renewal date. You may cancel at any time through [your account portal/Our website]. Upon cancellation, Premium features will remain active until the end of your paid period, then automatically revert to Free Tier.

### 6.5 Refund Policy
[Insert your refund policy here, e.g., "Refunds are available within 30 days of initial Premium subscription purchase. No refunds for subsequent renewals."]

### 6.6 Price Changes
We reserve the right to change subscription prices with [30] days' notice. Price changes do not affect your current subscription period.

### 6.7 Voluntary Downgrade
You may voluntarily downgrade from Premium to Free Tier at any time. Upon downgrade:
- Premium features will be disabled at the end of your paid period
- Your certificate will be revoked
- Free Tier features will remain fully functional
- Data created with Premium features may become inaccessible or limited

---

## 7. ACCOUNT TERMINATION AND LICENSE REVOCATION

### 7.1 Free Tier - No Termination
Free Tier access cannot be terminated by Us except for violation of this Agreement. You may uninstall the Software at any time.

### 7.2 Premium Tier - Voluntary Termination
You may terminate your Premium subscription at any time by:
- Canceling through your account portal
- Requesting account deletion
- Contacting customer support
- Simply allowing your subscription to expire

Upon Premium subscription termination:
- Premium features will be disabled
- Your certificate will be revoked
- The Software will revert to Free Tier functionality
- Your account data will be deleted per Our data retention policy

### 7.3 Premium Tier - Involuntary Termination
We may suspend or terminate your Premium subscription immediately if:
- You violate any term of this Agreement
- Your payment method fails repeatedly
- We detect fraudulent activity or license abuse
- We suspect certificate compromise or unauthorized sharing
- Required by law or legal process

Upon involuntary termination, Premium features will be disabled and the Software will revert to Free Tier.

### 7.4 Effect of Premium Termination
Upon Premium subscription termination for any reason:
- Your right to use Premium features immediately ceases
- Free Tier features remain fully functional
- All certificates and license tokens are revoked
- No refund of subscription fees (unless required by law)
- You may re-subscribe at any time

---

## 8. FEATURE COMPARISON

### 8.1 Free Tier Features
The Free Tier includes:
- [List basic features available in Free Tier]
- No network connectivity required
- No account creation required
- No data transmission to Our servers
- Unlimited use

### 8.2 Premium Tier Features
Premium subscription adds:
- [List advanced features available in Premium Tier]
- Priority customer support
- [Additional premium benefits]

### 8.3 Feature Changes
We reserve the right to:
- Add new features to either tier
- Move features between tiers with reasonable notice
- Discontinue features with reasonable notice
- Modify feature functionality

Existing Premium subscribers will be notified of material changes at least [30] days in advance.

---

## 9. INTELLECTUAL PROPERTY RIGHTS

### 9.1 Ownership
The Software, including all intellectual property rights, is and shall remain the exclusive property of the Company and its licensors. This Agreement does not grant You any ownership rights in the Software.

### 9.2 Trademarks
All trademarks, service marks, and trade names are proprietary to the Company or its licensors. You may not use any such marks without Our prior written consent.

### 9.3 Feedback
If You provide suggestions, ideas, or feedback about the Software, We may use such feedback without obligation or compensation to You.

---

## 10. WARRANTY DISCLAIMER

**THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND. TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, WE DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.**

**WE DO NOT WARRANT THAT:**
- **The Software will meet your requirements**
- **The Software will be uninterrupted, timely, secure, or error-free**
- **The results obtained from use of the Software will be accurate or reliable**
- **Any errors in the Software will be corrected**
- **Premium features will always be available or function identically to Free features**

**THE ENTIRE RISK ARISING OUT OF USE OR PERFORMANCE OF THE SOFTWARE REMAINS WITH YOU.**

---

## 11. LIMITATION OF LIABILITY

**TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE COMPANY BE LIABLE FOR ANY SPECIAL, INCIDENTAL, INDIRECT, OR CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS) ARISING OUT OF THE USE OF OR INABILITY TO USE THE SOFTWARE, EVEN IF THE COMPANY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.**

**FOR FREE TIER USERS: OUR ENTIRE LIABILITY SHALL BE LIMITED TO $0 (ZERO DOLLARS).**

**FOR PREMIUM TIER USERS: OUR ENTIRE LIABILITY SHALL BE LIMITED TO THE AMOUNT YOU ACTUALLY PAID FOR THE PREMIUM SUBSCRIPTION IN THE TWELVE (12) MONTHS PRECEDING THE CLAIM.**

**SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THE ABOVE LIMITATION OR EXCLUSION MAY NOT APPLY TO YOU.**

---

## 12. UPDATES AND MODIFICATIONS

### 12.1 Software Updates
We may provide updates, patches, or new versions of the Software. Updates may be:
- Automatically downloaded and installed
- Optional or required for continued use
- Subject to acceptance of amended terms
- Available to all users or Premium subscribers only

### 12.2 Agreement Changes
We reserve the right to modify this Agreement at any time. Changes will be effective upon:
- Posting the updated Agreement on Our website
- Notice provided through the Software or via email (Premium users)
- Your continued use of the Software after notice

Material changes will require your acceptance before continued use. Changes affecting Premium subscribers will be communicated via email.

---

## 13. EXPORT COMPLIANCE

You agree to comply with all applicable export and import laws and regulations. You may not export or re-export the Software to any country, person, or entity subject to export restrictions under applicable law.

---

## 14. GOVERNMENT END USERS

If You are a U.S. Government end user, the Software is a "commercial item" as defined in 48 C.F.R. 2.101, consisting of "commercial computer software" and "commercial computer software documentation" as such terms are used in 48 C.F.R. 12.212. Government users have only those rights granted to all other users under this Agreement.

---

## 15. GENERAL PROVISIONS

### 15.1 Entire Agreement
This Agreement constitutes the entire agreement between You and Us regarding the Software and supersedes all prior agreements and understandings.

### 15.2 Severability
If any provision of this Agreement is held to be invalid or unenforceable, the remaining provisions shall remain in full force and effect.

### 15.3 Waiver
Our failure to enforce any provision of this Agreement shall not constitute a waiver of that provision or any other provision.

### 15.4 Assignment
You may not assign or transfer this Agreement or any rights under it. We may assign this Agreement without restriction.

### 15.5 Governing Law
This Agreement shall be governed by and construed in accordance with the laws of [Jurisdiction], without regard to its conflict of laws principles.

### 15.6 Dispute Resolution
Any disputes arising under this Agreement shall be resolved through [binding arbitration/mediation/courts of Jurisdiction].

### 15.7 Attorney's Fees
In any legal action to enforce this Agreement, the prevailing party shall be entitled to recover reasonable attorney's fees and costs.

### 15.8 Force Majeure
We shall not be liable for any failure or delay in performance due to causes beyond Our reasonable control.

---

## 16. CONTACT INFORMATION

If you have questions about this Agreement, please contact us at:

**[Your Company Name]**  
[Address]  
[City, State, ZIP]  
Email: [support email]  
Phone: [phone number]  
Website: [website]

---

## 17. ACKNOWLEDGMENT

**BY CLICKING "I AGREE," INSTALLING, OR USING THE SOFTWARE, YOU ACKNOWLEDGE THAT:**

1. You have read and understand this Agreement
2. You agree to be bound by its terms and conditions
3. You understand that Free Tier operates completely offline with no data collection
4. You understand that Premium Tier requires network connectivity for license validation
5. You consent to the Premium Tier data collection practices described herein (if subscribing)
6. You are authorized to enter into this Agreement
7. You are at least 18 years of age (or the age of majority in your jurisdiction)
8. You understand that canceling Premium subscription reverts the Software to Free Tier

**IF YOU DO NOT AGREE TO THESE TERMS, DO NOT INSTALL OR USE THE SOFTWARE.**

---

**Copyright © [Year] [Your Company Name]. All Rights Reserved.**