/**
 * Deploy this as a Google Apps Script Web App:
 * 1. drive.google.com → New → Google Apps Script
 * 2. Paste this file
 * 3. Deploy → New deployment → Web app
 *    - Execute as: Me
 *    - Who has access: Anyone
 * 4. Copy the web app URL into Court Balance → Admin → Apps Script sync URL
 *
 * The script stores tournament JSON (including admin credentials) in a Drive file
 * named "court-balance-data.json".
 */

var FILE_NAME = 'court-balance-data.json';

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) || 'load';
  if (action === 'load') {
    return ContentService
      .createTextOutput(loadJson_())
      .setMimeType(ContentService.MimeType.JSON);
  }
  return ContentService.createTextOutput('{"error":"unknown action"}')
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  var action = (e && e.parameter && e.parameter.action) || 'save';
  if (action === 'save') {
    var body = (e && e.postData && e.postData.contents) || '{}';
    saveJson_(body);
    return ContentService
      .createTextOutput('{"ok":true}')
      .setMimeType(ContentService.MimeType.JSON);
  }
  return ContentService.createTextOutput('{"error":"unknown action"}')
    .setMimeType(ContentService.MimeType.JSON);
}

function loadJson_() {
  var file = findFile_();
  if (!file) {
    return JSON.stringify({
      players: [],
      teams: [],
      admin: { username: 'admin', password: 'volleyball' },
      pendingSwitch: null,
      driveFileId: '',
      driveSyncUrl: '',
      lastSyncedAt: 0
    });
  }
  return file.getBlob().getDataAsString();
}

function saveJson_(json) {
  var file = findFile_();
  if (file) {
    file.setContent(json);
  } else {
    DriveApp.createFile(FILE_NAME, json, MimeType.PLAIN_TEXT);
  }
}

function findFile_() {
  var files = DriveApp.getFilesByName(FILE_NAME);
  return files.hasNext() ? files.next() : null;
}
