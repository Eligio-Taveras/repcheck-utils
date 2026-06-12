package com.repcheck.utils.errors

import java.net.ConnectException
import java.util.concurrent.TimeoutException

/**
 * The composition every HTTP-backed subsystem keeps re-writing: request timeouts and refused connections (a server
 * restarting or still warming up) are transient, transient HTTP statuses (429/5xx) are recognized through the
 * subsystem's `statusExtractor`, and everything else is systemic — fail fast. Subsystems supply only the extractor that
 * maps their flat wire error onto its status code.
 */
final class NetworkAwareHttpClassifier(statusExtractor: Throwable => Option[Int]) extends ErrorClassifier {

  private val httpStatus = new HttpErrorClassifier(statusExtractor)

  def classify(error: Throwable): ErrorClass =
    error match {
      case _: TimeoutException => ErrorClass.Transient
      case _: ConnectException => ErrorClass.Transient
      case other               => httpStatus.classify(other)
    }

}
