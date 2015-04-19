# http-kit-view-demo

A Clojure library designed to ... well, that part is up to you.

## How views work

There are three major sub-systems in the views library.

  1. Subscriptions
  2. View Templates
  3. Hints

When an update happens, a hint is sent out.  Refresh-views is called and the hint will be picked up and refresh-views will be called so that each view in the system can be checked against the hints which came in, using the 'relevant' function defined by the view type which extends IView.

If any hints are relevant for a view, a job is placed on a queue. The worker thread or threads (depending on config) periodically (once a minute, hard-coded as of now) poll this queue and whenever a job comes up, the 'data' function implemented by the view type extending IView is called, and the :send-fn, as configured in the view-config, is called with the output of data.

So, what is required to get a working views system:

- a view-config (view-system) must be initialized with the following key/value pairs:
  - a send-fn for sending out initial and update data
    :send-fn (fn [unique-subscriber-key [[view-id view-parameters] view-data]])
  - a hash-map of views- each one a key/value pair with a view-id (can be anything used as a key for a map) and a value which is an instance of a type implementing IView.
    :views   {:view1 (MyView. ...) :view2 (MyView. ...)}

- views.core/update-watcher! must be called to initialize the refresh queue and hint listener threads.

- an implementation or implementations of IView for populating the :views map as described in the views-system

- some way to push hints out so they are picked up by the listeners and checked against views in the system.  In the case of our HoneySQL-based database-specific view update wrappers, we send the table names as keywords as hints, and the relevant function for a HSQLView checks if the table names (hints) match any of the joins or main tables in the current set of subscribed views.

- a way to subscribe to views...

## Usage

FIXME

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
