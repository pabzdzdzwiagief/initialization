// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization;

/**
 * An annotation type for persisting traces from Annotations trait inside
 * classfiles.
 *
 * The plugin starts adding annotations long after `pickler` compilation
 * phase, when any new StaticAnnotations/ClassfileAnnotations are no longer
 * going to be persisted in classfiles. For some reason however Java
 * annotations still are (but only when attached to a type symbol, hence
 * presence of `from` attributes even though just attaching to a relevant method
 * would be more obvious).
 */
@interface Trace {
    /** A canonical name of member's owner (type), needed for static calls. */
    String owner();

    /** Method/field symbol action on which this Trace describes. */
    String memberName();

    /** Used to select alternative symbol in case of overloading
     *  (assuming tpe.safeToString uniqueness here). */
    String typeString();

    /** Method symbol action in which this Trace describes. */
    String fromMemberName();

    /** Like `typeString`, but for `fromMemberName` symbol. */
    String fromTypeString();

    /** Short name of a subclass of Annotations.Trace. */
    String traceType();

    /** As in Annotations.Trace. */
    int point();

    /** As in Annotations.Trace. */
    int ordinal();
}
