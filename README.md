# Overview

This sample app for Android uses the OAuth API of High-Mobility to have car access granted by the owner. The OAuth flow is described in detail on https://high-mobility.com/learn/documentation/cloud-api/oauth2/intro/

# Configuration

Before running the app, make sure to configure the following:

1. in `MainActivity.kt`: Initialise HMKit with a valid device certificate from the Developer Center https://high-mobility.com/develop/
2. In the Developer Center, go to the `OAuth2 Client` settings page
3. Copy and insert the Client ID, Client Secret, Auth URI, Token URI, URL Scheme, App Id and scope into the instance variables of `OAuthManager.kt`. No worries, all of this is written on OAuth page
4. Set the scopes for which you ask permissions - the full list is here https://high-mobility.com/learn/documentation/auto-api/api-structure/permissions

# Run the app

Run the app on your phone and start the OAuth process. Once completed you will receive an `Access Token` that is passed into HMKit to download access certificates for the car. With this, the device has been authorised.
