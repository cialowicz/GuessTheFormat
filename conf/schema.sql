CREATE DATABASE IF NOT EXISTS gtf;

USE gtf;

CREATE TABLE IF NOT EXISTS cameras (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exifMake VARCHAR(64),
  exifModel VARCHAR(64),
  format VARCHAR(16),
  displayText VARCHAR(256),
  adCode VARCHAR(2048)
);
CREATE INDEX camera_format ON cameras (format);
CREATE UNIQUE INDEX camera_model ON cameras (exifModel);

CREATE TABLE IF NOT EXISTS unknownCameras (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exifMake VARCHAR(64),
  exifModel VARCHAR(64),
  count int(11) unsigned NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX unknown_camera_model ON unknownCameras (exifMake, exifModel);

CREATE TABLE IF NOT EXISTS photos (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  flickrPhotoId VARCHAR(64) NOT NULL,
  url VARCHAR(256) NOT NULL,
  imageUrlMd VARCHAR(256) NOT NULL,
  imageUrlLg VARCHAR(256) NOT NULL,
  title VARCHAR(256),
  username VARCHAR(128) NOT NULL,
  userId VARCHAR(64) NOT NULL,
  fullName VARCHAR(256),
  cameraId INT NOT NULL,
  format VARCHAR(16) NOT NULL,
  shutter VARCHAR(16),
  aperture VARCHAR(16),
  iso VARCHAR(16),
  focalLength VARCHAR(16),
  exploreDate DATE NOT NULL,
  exploreNumber INT NOT NULL
);
ALTER TABLE photos ADD CONSTRAINT fk_cameraId FOREIGN KEY (cameraId) references cameras(id);
CREATE INDEX photo_camera ON photos (cameraId);
CREATE UNIQUE INDEX photo_flickrId ON photos (flickrPhotoId);
CREATE INDEX photo_format ON photos (format);
CREATE INDEX photo_format_date ON photos (format, exploreDate);
CREATE INDEX photo_format_number ON photos (format, exploreNumber);
CREATE INDEX photo_format_date_number ON photos (format, exploreDate, exploreNumber);

CREATE TABLE IF NOT EXISTS stats (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	date DATE NOT NULL,
	correctFormat VARCHAR(16) NOT NULL,
	incorrectFormat VARCHAR(16) NOT NULL,
	impressions int(11) unsigned NOT NULL DEFAULT 0,
	skipped int(11) unsigned NOT NULL DEFAULT 0,
	guesses int(11) unsigned NOT NULL DEFAULT 0,
	correct int(11) unsigned NOT NULL DEFAULT 0
);
CREATE INDEX stats_correct_incorrect ON stats (correctFormat, incorrectFormat);
CREATE UNIQUE INDEX stats_correct_incorrect_date ON stats (date, correctFormat, incorrectFormat);

CREATE TABLE IF NOT EXISTS statsSummary (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	correctFormat VARCHAR(16) NOT NULL,
	incorrectFormat VARCHAR(16) NOT NULL,
	impressions int(11) unsigned NOT NULL DEFAULT 0,
	skipped int(11) unsigned NOT NULL DEFAULT 0,
	guesses int(11) unsigned NOT NULL DEFAULT 0,
	correct int(11) unsigned NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX stats_correct_incorrect ON statsSummary(correctFormat, incorrectFormat);

-- load camera data into cameras table
LOAD DATA LOCAL INFILE 'cameras.csv'
REPLACE INTO TABLE cameras
FIELDS TERMINATED BY ',' ENCLOSED BY "'"
IGNORE 1 LINES
(id, exifMake, exifModel, format, displayText, adCode);

-- get camera data from cameras
SELECT id, exifMake, exifModel, format, displayText, adCode
FROM cameras
INTO OUTFILE 'cameras.csv'
    FIELDS TERMINATED BY ',' ENCLOSED BY "'"
    LINES TERMINATED BY '\n';
