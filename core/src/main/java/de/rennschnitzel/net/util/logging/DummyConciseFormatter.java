package de.rennschnitzel.net.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.google.common.base.Strings;

// Copyright BungeeCord

/*
 * Copyright (c) 2012, md_5. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * The name of the author may not be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * You may not use the software for commercial software hosting services without written permission
 * from the author.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class DummyConciseFormatter extends Formatter {

  private static int maxLength = 4;

  private final DateFormat date = new SimpleDateFormat("HH:mm:ss");

  @Override
  public String format(LogRecord record) {

    int spaces = maxLength - 4; // null

    if (record.getLoggerName() != null) {
      int len = record.getLoggerName().length();
      if (maxLength < len) {
        maxLength = len;
      }
      spaces = maxLength - len + 1;
    }
    StringBuilder formatted = new StringBuilder();

    //formatted.append(date.format(record.getMillis()));
    //formatted.append(" [");
    //formatted.append(record.getLevel().getName());
    //formatted.append("]");
    formatted.append("[");
    formatted.append(record.getLoggerName());
    formatted.append("]");
    formatted.append(Strings.repeat(" ", spaces));
    formatted.append(formatMessage(record));
    if (record.getThrown() != null) {
      StringWriter writer = new StringWriter();
      record.getThrown().printStackTrace(new PrintWriter(writer));
      formatted.append(writer);
    }

    return formatted.toString();
  }
}
