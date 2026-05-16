/**
 * A/B comparison and report generation for audio recordings.
 *
 * <p>{@link org.hammer.audio.compare.RecordingComparator} replays two recordings, runs the standard
 * measurement, spectrum and diagnosis analyzers on each, and produces an immutable {@link
 * org.hammer.audio.compare.ComparisonReport}. {@link
 * org.hammer.audio.compare.MarkdownComparisonReportRenderer} formats the report as Markdown
 * suitable for QA notes or bug tickets.
 */
package org.hammer.audio.compare;
