# Basic OAuth

This sample app for Android uses the OAuth API of High-Mobility to have car access granted by the 
owner. The OAuth flow is described in detail on https://high-mobility.com/learn/documentation/cloud-api/oauth2/intro/

# Configuration

Before running the app, make sure to configure the following:

1. in `BasicOAuthActivity.kt`: Initialise HMKit with a valid device certificate from the Developer 
Center https://high-mobility.com/develop/
2. In the Developer Center, go to the `OAuth2 Client` settings page
3. Copy and insert the Auth URI, Client ID, redirectScheme (URL-scheme for iOS & Android), Token URI,
and an app's Serial # / AppId, into the instance variables of `BasicOAuthActivity.kt`. All of this 
is written on the OAuth page: https://high-mobility.com/profile/oauth-client
4. Set the scopes for which you ask permissions - the full list is here https://high-mobility.com/learn/documentation/auto-api/api-structure/permissions

# Run the app

Run the app on your phone and start the OAuth process. Once completed you will receive an `Access Token`
that is passed into HMKit to download access certificates for the car. With this, the device has been authorised.

# Questions or Comments ?

If you have questions or if you would like to send us feedback, join our [Slack Channel](https://slack.high-mobility.com/)
or email us at [support@high-mobility.com](mailto:support@high-mobility.com).
