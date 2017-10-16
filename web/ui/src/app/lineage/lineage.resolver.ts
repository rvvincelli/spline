/*
 * Copyright 2017 Barclays Africa Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Injectable} from "@angular/core";
import {ActivatedRouteSnapshot, Resolve} from "@angular/router";
import {IDataLineage} from "../../generated-ts/lineage-model";
import {LineageService} from "./lineage.service";

@Injectable()
export class LineageByIdResolver implements Resolve<IDataLineage> {

    constructor(private lineageService: LineageService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<IDataLineage> {
        let lineageId = route.paramMap.get('id')
        return this.lineageService.getLineage(lineageId)
    }

}