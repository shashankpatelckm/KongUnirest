/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

class RetryAfter {
    private static DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_DATE)
            .appendPattern("XX")
            .toFormatter();
    private int millies;

    public RetryAfter(int seconds) {
        this.millies = seconds * 1000;
    }

    public static RetryAfter parse(Headers response) {
        String value = response.getFirst("Retry-After");
        return tryAsInt(value)
                .orElse(tryAsDateTime(value));
    }

    private static RetryAfter tryAsDateTime(String value) {
        return new RetryAfter(1);
    }

    private static Optional<RetryAfter> tryAsInt(String s){
        try{
            return Optional.of(new RetryAfter(Integer.parseInt(s)));
        }catch (NumberFormatException e){
            return Optional.empty();
        }
    }


    public void waitForIt() {
        try {
            Thread.currentThread().sleep(millies);
        } catch (InterruptedException e) {
            throw new UnirestException(e);
        }
    }
}
