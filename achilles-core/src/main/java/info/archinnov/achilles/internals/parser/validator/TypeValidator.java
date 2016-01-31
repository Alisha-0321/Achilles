/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.parser.validator;

import static info.archinnov.achilles.internals.parser.TypeUtils.ALLOWED_TYPES;

import com.squareup.javapoet.*;

import info.archinnov.achilles.internals.apt.AptUtils;

public class TypeValidator {

    public static void validateAllowedTypes(AptUtils aptUtils, TypeName parentType, TypeName type) {
        if (type.isPrimitive()) {
            return;
        } else if (type instanceof ArrayTypeName) {
            final TypeName componentType = ((ArrayTypeName) type).componentType;
            final boolean isByteArray = componentType.unbox().toString().equals(byte.class.getCanonicalName());
            aptUtils.validateTrue(isByteArray, "Type '%s' in '%s' is not a valid type for CQL", type.toString(), parentType.toString());
        } else if (type instanceof ParameterizedTypeName) {
            final ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) type;
            validateAllowedTypes(aptUtils, parentType, parameterizedTypeName.rawType);

            for (TypeName x : parameterizedTypeName.typeArguments) {
                validateAllowedTypes(aptUtils, parentType, x);
            }
        } else if (type instanceof WildcardTypeName) {
            final WildcardTypeName wildcardTypeName = (WildcardTypeName) type;
            for (TypeName x : wildcardTypeName.upperBounds) {
                validateAllowedTypes(aptUtils, parentType, x);
            }
        } else if (type instanceof ClassName) {
            final ClassName className = (ClassName) type;
            final boolean isValidType = ALLOWED_TYPES.contains(className);
            aptUtils.validateTrue(isValidType, "Type '%s' in '%s' is not a valid type for CQL", type.toString(), parentType.toString());
        } else {
            aptUtils.printError("Type '%s' in '%s' is not a valid type for CQL", type.toString(), parentType.toString());
        }
    }
}
