# Ad-Tech

An exercise in Scala/Akka recording clicks/impressions for 'net advertising.

Specifically, this is probably what most people would call a __microservice__
too (it has virtually no external dependencies).

## Internet Advertising

Internet advertising is sold in two forms: by the click and by the impression.

The first is referred to as __PPC__ (Pay Per Click) and the second is __PPM__
(Pay Per Mille, where Mille by convention means a thousand impressions).
The general gist is that more eyeballs on an advertisement are generally
better than fewer. And of course it's ___really___ important to get those
advertisements in front of the ___right___ eyeballs. So demographic and
user profiles are valuable information (just ask Facebook or Google).

As should be clear by now, this is a business that is based upon __volume__.
And of course the goal of all this advertising is name brand recognition,
but perhaps more importantly: __clickthrough__ (or __conversion__). Or
in other words, how many people were actually motivated to part with their
cash and actually ___buy___ something by the advertisement. And all of this
needs to be tracked, so that the advertising agency or Amazon/Facebook/Google
can get paid.

## Scala / Akka

For some reason, Scala has a reputation for being difficult. I'm not sure why,
compared with C++ or Lisp or Scheme or Ops5 it's actually pretty simple. Sure,
purely functional or reactive or reactive/functional programming can be very
hard (conceptually at least it can be hard to break old habits) - but these
are not _imposed_ by Scala. It's entirely possible to write Scala code that
looks almost exactly like the Java code that it is supposed to replace.
In fact it is exactly possible, since Scala runs on a JVM (Java Virtual
Machine).

Be that as it may, __Akka__ is definitely a pretty heavy-duty framework.
Here I used Akka (specifically `akka-http`) simply due to the very high-order
transaction volume.

[I had previously used __Spray__ - which is a framework much like __node.js__
or Python's __Flask__ or even Golang's Gorilla/mux. However, Spray is now
deprecated so I decided to use __akka-http__ which is its replacement (another
option might have been __Play__).]

## Specification

I am including the [specification](./INSTRUCTIONS.md) I was given.

## How to build the service

Run the following command to build the service:

    $ sbt test

This will also run a test suite:

    < ... >
    [info] Done compiling.
    [info] AnalyticsServiceSuite:
    [info] - should return 0 records in response to get Statistics (9-10 PM Pre-New Year)
    [info] - should return 1 record in response to get Statistics (10-11 PM Pre-New Year)
    [info] - should respond to get Statistics (New Year)
    [info] - should respond to get Statistics pre-New Year
    [info] - should respond to get Statistics (1 AM)
    [info] - should respond to post Statistic
    [info] - should use cached Statistics (First 1 AM)
    [info] - should NOT use cached Statistics (First)
    [info] - should use cached Statistics (Second 1 AM)
    [info] - should NOT use cached Statistics (Second)
    [info] Run completed in 6 seconds, 389 milliseconds.
    [info] Total number of tests run: 10
    [info] Suites: completed 1, aborted 0
    [info] Tests: succeeded 10, failed 0, canceled 0, ignored 0, pending 0
    [info] All tests passed.
    [success] Total time: 12 s, completed 22-Mar-2018 9:24:30 PM
    $

## How to run the service

Run the following command:

    $ sbt run

The service runs on port 8080 by default and should be available at:

    http://localhost:8080/analytics

It may also be tested with `curl`.

[Press Enter or Return to terminate.]

## Testing with curl

The procedure will be to create an analytic, and then query for that analytic.

#### Create an analytic

To create an analytic for the given timestamp:

    ```
    $  curl -v -X POST 127.0.0.1:8080/analytics?timestamp=1514793600000\&user=Fred\&event=impression
    *   Trying 127.0.0.1...
    * Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
    > POST /analytics?timestamp=1514793600000&user=Fred&event=impression HTTP/1.1
    > Host: 127.0.0.1:8080
    > User-Agent: curl/7.47.0
    > Accept: */*
    > 
    < HTTP/1.1 204 No Content
    < Server: akka-http/10.1.0
    < Date: Fri, 23 Mar 2018 04:10:07 GMT
    < 
    * Connection #0 to host 127.0.0.1 left intact
    $
    ```

#### Get analytics

To get analytics for the current hour of the timestamp, up
to and including the actual millisecond of the timestamp:

    ```
    $ curl -v 127.0.0.1:8080/analytics?timestamp=1514793600000
    *   Trying 127.0.0.1...
    * Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
    > GET /analytics?timestamp=1514793600000 HTTP/1.1
    > Host: 127.0.0.1:8080
    > User-Agent: curl/7.47.0
    > Accept: */*
    > 
    < HTTP/1.1 200 OK
    < Server: akka-http/10.1.0
    < Date: Fri, 23 Mar 2018 04:10:55 GMT
    < Content-Type: text/plain; charset=UTF-8
    < Content-Length: 38
    < 
    unique_users,1
    clicks,0
    impressions,1
    * Connection #0 to host 127.0.0.1 left intact
    $
    ```
