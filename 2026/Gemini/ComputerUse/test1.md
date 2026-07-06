# GUI Automation Test Procedure: MFP & RMM Integration

- **Target MFP**: 10.36.104.40
- **Target Server**: https://rmmmfpdevdata.z13.web.core.windows.net/TestUI#

## Step 1: Obtain SCEP URL from Test Server
- Action: On the test server website (currently open in Edge), navigate to "Device" -> "MFP/Printer" -> "AddDevice", click "Copy URL", and copy the connection URL.
- Expected: The SCEP connection URL is successfully copied to the clipboard.

## Step 2: Configure MFP FSS Settings and Reboot
- Action: Open a new tab, navigate to the MFP web page (http://10.36.104.40), log in, go to the Extended FSS settings, paste the copied SCEP URL, and reboot the MFP.
- Expected: SCEP URL is pasted, saved, and the MFP starts rebooting.

## Step 3: Configure Certificate Server on RMM and Verify Update
- Action: Configure the SCEP server settings in RMM, trigger a "Check Now" on the MFP, and verify that the certificate is updated successfully on the RMM side.
- Expected: The certificate status is successfully updated on the RMM portal.
