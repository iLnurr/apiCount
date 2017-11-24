
https://drive.google.com/open?id=1vBBZ1EfSsj1HjhFPkMHmE99xjjsC7zr879A6VgH_b4M

### Run app
`$ sbt clean compile`
`$ sbt test`

using
`$ sbt run`

or

`$ sbt assembly`

and
 
`$ java -jar target/scala-2.12/apiCount-assembly-0.1.jar`

### For testing
Install command-line http-client: httpie.

`$ sudo apt-get install httpie`

And then test http api

`$ http GET localhost:5000/search?tag=scala`