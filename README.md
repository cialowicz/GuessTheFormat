# GuessTheFormat

[GuessTheFormat](http://guesstheformat.com) (GTF) is an experiment for photographers: can you tell the difference between camera sensor formats just by looking at a photo?

[View a demo video of the application on YouTube](https://youtu.be/hWj2snGiCpU), or see [guesstheformat.com/about](http://guesstheformat.com/about) for more information. Since you're looking at this git repository, check out [app/views/about.scala.html](app/views/about.scala.html).

[![GuessTheFormat Animated Example Image](https://raw.githubusercontent.com/cialowicz/GuessTheFormat/master/public/images/example.gif)](https://youtu.be/hWj2snGiCpU)

## Notes

This README.md was written a full *four* years after this project was last worked on. The intent here is not to showcase my current programming skills, but to hand off this project to someone else who may want to take over, fork, or maintain it.

Please [contact me](https://twitter.com/MikeCialowicz) to become a contributor/collaborator, or just fork this repository.

## Project Structure

This is a [Scala Play Framework](https://www.playframework.com/documentation/2.2.x/Home) application. See the [build.sbt](build.sbt) file for library dependencies.

There are two main actions:

 1. Fetch a random photo from the DB: `GET /photo`
 1. Make a sensor format guess about a photo (and get a result back): `POST /guess/:photoId/:formatGuess/:otherFormat`

See the [conf/routes](conf/routes) file for more information about the each route.

There are two recurring jobs in [app/Global.scala](app/Global.scala) that will each run 30-seconds after server start-up, and then be scheduled to run periodically:

 1. Flickr "Explore" photo population: `controllers.PhotoController.populatePhotosFrom`. This is used to generate a DB cache of "Interesting" Flickr explore photos, which is used for random selection.
 1. Stats rollup, for the stats page ([app/views/stats/stats.scala.html](app/views/stats/stats.scala.html)): `controllers.StatsController.statsRollup`

The [conf/cameras.csv](conf/cameras.csv) file must be maintained and updated with the EXIF info of popular cameras (as found on Flickr). Disclaimer: this file currently has Amazon referral links for certain cameras.

## Configuration

The [conf/schema.sql](conf/schema.sql) contains the MySQL database schema that's required, and [conf/cameras.csv](conf/cameras.csv) contains the data that should be loaded into the `cameras` table.

 1. Set up a MySQL instance with a `gtf` database.
 1. [Generate a new application secret](https://www.playframework.com/documentation/2.5.x/ApplicationSecret#generating-an-application-secret).
 1. [Get a Flickr API key](https://www.flickr.com/services/api/misc.api_keys.html).
 1. Update the [conf/application.conf](conf/application.conf) and [conf/application.prod.conf](conf/application.prod.conf) files with your Play application secret, Flickr API keys, and MySQL instance connection details.
 1. Run the [conf/schema.sql](conf/schema.sql) against the `gtf` db.
 1. Populate the `cameras` table using the script below.

```
LOAD DATA LOCAL INFILE 'cameras.csv'
REPLACE INTO TABLE cameras
FIELDS TERMINATED BY ',' ENCLOSED BY "'"
IGNORE 1 LINES
(id, exifMake, exifModel, format, displayText, adCode);
```

## Running

    sbt run

 Or

    chmod +x gtf
    echo "-Xms256m -Xmx512m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=128m"
    ./gtf -Dconfig.resource=application.prod.conf -Dhttp.port=80 &

## Deployment

This application will run just fine on a single EC2 t2.micro instance.

 1. Follow the configuration instructions above, first (get your MySQL db set up and populated, update the config files, etc).
 1. Update the application version number in the [build.sbt](build.sbt) file.
 1. `play clean`
 1. `play compile`
 1. `play dist`
 1. `SCP /GTF/target/universal/GTF-<version>.zip` to location of choice
 1. unzip
 1. update `get_mem_opts()` in `/var/www/GTF-<version>/bin/gtf` to:
 ```
       get_mem_opts () {
         echo "-Xms256m -Xmx512m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=128m"
       }
```

## License

MIT. See [LICENSE.md](LICENSE.md).