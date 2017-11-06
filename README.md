# Sample chat webapp using Play and Akka Typed

I wrote this as a learning project to figure out how to use Akka Typed.

## Usage

Run `sbt run` and go to `localhost:9000`.

## Notes on writing this webapp

* [Immutable typed actors][actors_source] are identical to state machines, and
  they're really easy to reason about!
* The Akka Typed scaladocs aren't linked anywhere on the website, but you can
  find them [here][akka_typed_scaladoc].
* Since Play uses untyped actors by default, `import
  akka.typed.scaladsl.adapter._` is required to get interop with typed actors.
* When a typed actor is converted into an untyped actor, sending *any* message
  that doesn't match the type of the actor's behavior to the wrapped untyped
  actor will cause an error, as expected.
* Use `akka.typed.scaladsl.adapter.PropsAdapter(myBehavior)` to generate an
  untyped `Props` from a `Behavior`.
* The websocket handler only passes in `ClientCommand`, so it's impossible for
  the client to do things that only the server can do!
* The actor that handles the websocket messages has to change which messages it
  accepts when the user connects to a room. This means that the actor has to
  have the most general `Command` type, which will allow it to receive both
  internal and external messages.
* An unconnected client only has access to an actor that accepts `JoinRoom`,
  because that's the only thing an unconnected client can do--join a room.

Maybe I'll write a blog post about writing the chat webapp once I refactor the
code to be nicer.

[actors_source]: tree/src/main/scala/actors/ChatRoom.scala
[akka_typed_scaladoc]: https://doc.akka.io/api/akka/current/akka/typed/

## License

Copyright 2017 Bryan Tan

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
