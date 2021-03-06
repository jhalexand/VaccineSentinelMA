# Notification Script Examples

The scripts in this directory provide examples of how to
send notifications when invoked by the Vaccine Sentinel.
Just copy the script to be called "sendNotice" in the same
directory you run the Sentinel from and make sure it is
executable. Modify the scripts as necessary.

Any script will do for notification, and one could easily
invoke a simple script that then called one or more of
these existing scripts.

## sendNotice-emailToText

This script is a python script that can connect to an SMTP (mail
delivery) server and send an email. It can be used for any email,
although the intention is that you use it to send a message to
an email-to-text service. By default, the script is set up to
use gmail as a mail server, but you'll need to change that if
you don't use gmail. Fill in the following values near the top
of the script:

* Your username (frequently this is your full email address)
* Your password (see the note in the script about app-specific passwords for gmail)
* Your phone number's email address to send to (see the note in the script for how to make this)

You can also adjust the `smtp` variable with a new server, and
optionally the port.

## sendNotice-pushover

This is a python script that invokes [Pushover](https://pushover.net)
to send a push notification to an iOS or Android device.
For a small fee you can send unlimited notifications whereever
you have the app installed.

In your Pushover account, create a new Application/API Token
and name it whatever you like (e.g Sentinel). Fill in your
user key and your API token near the top of the script in
the Client creation.

## sendNotice-applescriptNotification

This simple bash script for MacOS uses AppleScript to create a Notification
that will appear at the top right corner of your screen and
register in the Notification Center.

