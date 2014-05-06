/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.core.model;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.List;

public class SymbolicExceptionInfo {

    String exClass;

    String message;

    List<String> stackTrace;

    SymbolicExceptionInfo cause;


    public String getExClass() {
        return exClass;
    }


    public void setExClass(String exClass) {
        this.exClass = exClass;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public List<String> getStackTrace() {
        return stackTrace;
    }


    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }


    public SymbolicExceptionInfo getCause() {
        return cause;
    }


    public void setCause(SymbolicExceptionInfo cause) {
        this.cause = cause;
    }


    @Override
    public int hashCode() {
        return exClass.hashCode() + (message != null ? message.hashCode() : 0);
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof SymbolicExceptionInfo
            && ZorkaUtil.objEquals(((SymbolicExceptionInfo)obj).exClass, exClass)
            && ZorkaUtil.objEquals(((SymbolicExceptionInfo)obj).message, message)
            && ZorkaUtil.objEquals(((SymbolicExceptionInfo)obj).stackTrace, stackTrace);
    }

    @Override
    public String toString() {
        return exClass + ": " + message;
    }
}
