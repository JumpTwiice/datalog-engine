package solver;

import ast.*;
import common.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class Transformer {

    public static long changeFactsAndRulesToEDGFormat(Map<Long, List<Atom>> facts, Map<Long, List<Rule>> rules, long counter, Map<Long, String> idToVar) {
        Set<Long> problematicPreds = new HashSet<>(rules.keySet());
        problematicPreds.retainAll(facts.keySet());
        for (var pred : problematicPreds) {
            var newPred = counter++;
            var varCounter = 0L;
            idToVar.get(pred);
            idToVar.put(newPred, idToVar.get(pred) + "_fact");

            facts.put(newPred, facts.get(pred));
            var factList = facts.remove(pred);
            List<Term> ids = new ArrayList<>();
            for (var ignored : factList.getFirst().ids) {
                ids.add(new Term(varCounter++, true));
            }
            rules.get(pred).add(new Rule(new Atom(pred, ids), new ArrayList<>(List.of(new Atom(newPred, ids)))));
        }
        return counter;
    }

    public static void changeFactsAndRulesToEDGFormat(Program p) {
        Set<Long> problematicPreds = new HashSet<>(p.rules.keySet());
        problematicPreds.retainAll(p.facts.keySet());
        for (var pred : problematicPreds) {
            var newPred = p.nextPred++;
            var varCounter = 0L;
            p.idToVar.get(pred);
            p.idToVar.put(newPred, p.idToVar.get(pred) + "_fact");

            p.facts.put(newPred, p.facts.get(pred));
            var factList = p.facts.remove(pred);
            var firstElem = factList.iterator().next();
            List<Term> ids = new ArrayList<>(firstElem.size());
            for (var i = 0; i < firstElem.size(); i++) {
                ids.add(new Term(varCounter++, true));
            }

            p.rules.get(pred).add(new Rule(new Atom(pred, ids), new ArrayList<>(List.of(new Atom(newPred, ids)))));
        }
    }

    public static void setEqSet(Program p) {
        for (var ruleSet : p.rules.values()) {
            for (var r : ruleSet) {
                r.equalitySet = new EqualitySet();
                for (int i = 0; i < r.body.size(); i++) {
                    var currentRelation = r.body.get(i);
                    for (int j = 0; j < currentRelation.ids.size(); j++) {
                        var currentTerm = currentRelation.ids.get(j);
                        if (currentTerm.isVar) {
                            r.equalitySet.setEqual(i, j, currentTerm.value);
                        } else {
                            r.equalitySet.setConstant(i, j, (int) currentTerm.value);
                        }
                    }
                }
            }
        }
    }

    private static AdornedAtom adornAtom(Atom a) {
        var adornment = a.ids.stream().map(x -> !x.isVar).collect(Collectors.toCollection(ArrayList::new));
        return new AdornedAtom(a.pred, a.ids, adornment);
    }

    private static AdornedAtom magic(AdornedAtom a) {
        List<Term> newIds = new ArrayList<>();
        for (var i = 0; i < a.ids.size(); i++) {
            if (a.isBoundArray.get(i)) {
                newIds.add(a.ids.get(i));
            }
        }
        return new AdornedAtom(a.pred, newIds, a.isBoundArray, true);
    }

    public static Program magicSets(Program p) {
        if (p.query == null) {
            throw new IllegalArgumentException("Query was null");
        }

        var adorned = adornment(p);
        var adornedProgram = adorned.x();
        var sips = adorned.y();
        Map<Long, List<Rule>> ruleMap = new HashMap<>();
        for (var rEntry : adornedProgram.rules.entrySet()) {
            var ruleSet = ruleMap.computeIfAbsent(rEntry.getKey(), x -> new ArrayList<>());

            for (var r : rEntry.getValue()) {
                var headAtom = (AdornedAtom) r.head;
                var magicHead = magic(headAtom);
                r.body.addFirst(magicHead);
//                r.body.addFirst(magicHead);
//                q(x): m(x), b(x,y)
                ruleSet.add(new Rule(r.head, r.body));
                var currentSip = sips.get(r);
                for (var sipEntry : currentSip) {
                    ArrayList<Atom> body;
                    if (sipEntry.leftSide.contains(headAtom)) {
                        body = new ArrayList<>();
                        body.add(magicHead);
                        sipEntry.leftSide.stream().filter(x -> x != headAtom).forEach(body::add);
                    } else {
                        body = new ArrayList<>(sipEntry.leftSide);
                    }
                    ruleSet.add(new Rule(magic(sipEntry.rightSide), body));
                }
            }
        }
        var queryMagic = magic((AdornedAtom) adornedProgram.query);
//        Set<List<Long>> queryFactSet = new HashSet<>();
//        var constantsInQuery = queryMagic.ids.stream().filter(x -> !x.isVar).map(x -> x.value).collect(Collectors.toCollection(ArrayList::new));
//        queryFactSet.add(constantsInQuery);
//        newProgram.facts.put(newProgram.nextPred++, queryFactSet);
        Program newProgram = new Program(p.facts, ruleMap, adornedProgram.query, adornedProgram.idToVar, adornedProgram.nextPred);
        Transformer.changeFactsAndRulesToEDGFormat(newProgram);
        var renamed = renamePred(newProgram, queryMagic);
        changeFactsAndRulesToEDGFormat(renamed);
        return renamed;
    }

    private static Program renamePred(Program p, AdornedAtom magicQuery) {
//        var newProgram = new Program(null, null, p.query, new HashMap<>(), 0L);
        var idCounter = 0L;
//        Map<Long, Map<List<Boolean>, Long>> adornedToNewId = new HashMap<>();
        Map<Long, Map<Boolean, Map<List<Boolean>, Long>>> adornedToNewId = new HashMap<>();
        Map<Long, List<Rule>> newRules = new HashMap<>();
        Map<Long, Set<List<Long>>> newFacts = new HashMap<>();
        Map<Long, String> idToVar = new HashMap<>();

//        Handle the facts
        for (var factEntry : p.facts.entrySet()) {
            var newId = idCounter++;

            Map<List<Boolean>, Long> newMap = new HashMap<>();
            newMap.put(null, newId);
            Map<Boolean, Map<List<Boolean>, Long>> magicMap = new HashMap<>();
            magicMap.put(false, newMap);
            adornedToNewId.put(factEntry.getKey(), magicMap);
            idToVar.put(newId, p.idToVar.get(factEntry.getKey()));
            newFacts.put(newId, factEntry.getValue());
        }
//        Add the magicQuery as a fact
        var magicQueryID = idCounter++;
        Map<List<Boolean>, Long> newMap = new HashMap<>();
        newMap.put(magicQuery.isBoundArray, magicQueryID);

        Map<Boolean, Map<List<Boolean>, Long>> queryMagicMap = new HashMap<>();
        queryMagicMap.put(true, newMap);

        adornedToNewId.put(magicQuery.pred, queryMagicMap);
        idToVar.put(magicQueryID, magicQuery.generatePred(p));
//        idToVar.put(magicQueryID, magicQuery p.idToVar.get(magicQuery.pred) + "_" + boolArrToBoundString(magicQuery.isBoundArray));
        Set<List<Long>> queryFactSet = new HashSet<>();
        var constantsInQuery = magicQuery.ids.stream().map(x -> x.value).collect(Collectors.toCollection(ArrayList::new));
        queryFactSet.add(constantsInQuery);
        newFacts.put(magicQueryID, queryFactSet);

//        Handle the rules
        for (var ruleEntry : p.rules.entrySet()) {
            for (var r : ruleEntry.getValue()) {
                var headAtom = (AdornedAtom) r.head;
                var headCounterNewIdNewAtom =  renameAtom(p, adornedToNewId, headAtom, idCounter, idToVar);
                idCounter = headCounterNewIdNewAtom.x();
                long headPred = headCounterNewIdNewAtom.y().x();
                var newHead = headCounterNewIdNewAtom.y().y();
//                var innerMap = adornedToNewId.computeIfAbsent(headAtom.pred, x -> new HashMap<>());
//                var magicMap = innerMap.computeIfAbsent(headAtom.isMagic, x -> new HashMap<>());
//                long headPred;
//                if (!magicMap.containsKey(headAtom.isBoundArray)) {
//                    headPred = idCounter++;
//                    idToVar.put(headPred, headAtom.generatePred(p));
////                    idToVar.put(headPred, p.idToVar.get(headAtom.pred) + "_" + boolArrToBoundString(headAtom.isBoundArray));
//                } else {
//                    headPred = magicMap.get(headAtom.isBoundArray);
//                }
//                var newHead = new Atom(headPred, headAtom.ids);
                ArrayList<Atom> newBody = new ArrayList<>(r.body.size());
                for (var b_A : r.body) {
                    var bodyAtom = (AdornedAtom) b_A;
                    var bodyCounterNewIdNewAtom = renameAtom(p, adornedToNewId, bodyAtom, idCounter, idToVar);
                    idCounter = bodyCounterNewIdNewAtom.x();
                    newBody.add(bodyCounterNewIdNewAtom.y().y());
                }
                var ruleSet = newRules.computeIfAbsent(headPred, x -> new ArrayList<>());
                ruleSet.add(new Rule(newHead, newBody));
            }
        }
//        Handle query
        var queryAtom = (AdornedAtom) p.query;
        var innerMap = adornedToNewId.computeIfAbsent(queryAtom.pred, x -> new HashMap<>());
        var bodyMagicMap = innerMap.computeIfAbsent(queryAtom.isMagic, x -> new HashMap<>());
        long queryPred;
        if (!bodyMagicMap.containsKey(queryAtom.isBoundArray)) {
            queryPred = idCounter++;
            idToVar.put(queryPred, queryAtom.generatePred(p));
//            idToVar.put(queryPred, p.idToVar.get(queryAtom.pred) + "_" + boolArrToBoundString(headAtom.isBoundArray));
        } else {
            queryPred = bodyMagicMap.get(queryAtom.isBoundArray);
        }
        var newQuery = new Atom(queryPred, queryAtom.ids);

        return new Program(newFacts, newRules, newQuery, idToVar, idCounter);
    }

    private static Tuple<Long, Tuple<Long, Atom>> renameAtom(Program p, Map<Long, Map<Boolean, Map<List<Boolean>, Long>>> adornedToNewId, AdornedAtom oldAtom, long idCounter, Map<Long, String> idToVar) {
        long newPred;
        var innerMap = adornedToNewId.computeIfAbsent(oldAtom.pred, x -> new HashMap<>());
        var bodyMagicMap = innerMap.computeIfAbsent(oldAtom.isMagic, x -> new HashMap<>());
        if (!bodyMagicMap.containsKey(oldAtom.isBoundArray)) {
            newPred = idCounter++;
            bodyMagicMap.put(oldAtom.isBoundArray, newPred);
            idToVar.put(newPred, oldAtom.generatePred(p));
        } else {
            newPred = bodyMagicMap.get(oldAtom.isBoundArray);
        }
        var newBodyAtom = new Atom(newPred, oldAtom.ids);
        return new Tuple<>(idCounter, new Tuple<>(newPred, newBodyAtom));
    }

//    private static Program renamePred(Program p, AdornedAtom magicQuery) {
////        var newProgram = new Program(null, null, p.query, new HashMap<>(), 0L);
//        var idCounter = 0L;
//        Map<Long, Map<List<Boolean>, Long>> adornedToNewId = new HashMap<>();
//        Map<Long, Map<Boolean, Map<List<Boolean>, Long>> adornedToNewId = new HashMap<>();
//        Map<Long, List<Rule>> newRules = new HashMap<>();
//        Map<Long, Set<List<Long>>> newFacts = new HashMap<>();
//        Map<Long, String> idToVar = new HashMap<>();
//
////        Handle the facts
//        for (var factEntry : p.facts.entrySet()) {
//            var newId = idCounter++;
//            Map<List<Boolean>, Long> newMap = new HashMap<>();
//            newMap.put(null, newId);
//            adornedToNewId.put(newId, newMap);
//            idToVar.put(newId, p.idToVar.get(factEntry.getKey()));
//            newFacts.put(newId, factEntry.getValue());
//        }
//        System.out.println(idToVar);
////        Add the magicQuery as a fact
//        var magicQueryID = idCounter++;
//        Map<List<Boolean>, Long> newMap = new HashMap<>();
//        newMap.put(magicQuery.isBoundArray, magicQueryID);
//        adornedToNewId.put(magicQueryID, newMap);
//        idToVar.put(magicQueryID, magicQuery.generatePred(p));
////        idToVar.put(magicQueryID, magicQuery p.idToVar.get(magicQuery.pred) + "_" + boolArrToBoundString(magicQuery.isBoundArray));
//        Set<List<Long>> queryFactSet = new HashSet<>();
//        var constantsInQuery = magicQuery.ids.stream().map(x -> x.value).collect(Collectors.toCollection(ArrayList::new));
//        queryFactSet.add(constantsInQuery);
//        newFacts.put(magicQueryID, queryFactSet);
//
//
////        Handle the rules
//        for (var ruleEntry : p.rules.entrySet()) {
//            for (var r : ruleEntry.getValue()) {
//                var headAtom = (AdornedAtom) r.head;
//                var innerMap = adornedToNewId.computeIfAbsent(headAtom.pred, x -> new HashMap<>());
//                long headPred;
//                if (!innerMap.containsKey(headAtom.isBoundArray)) {
//                    headPred = idCounter++;
//                    idToVar.put(headPred, headAtom.generatePred(p));
////                    idToVar.put(headPred, p.idToVar.get(headAtom.pred) + "_" + boolArrToBoundString(headAtom.isBoundArray));
//                } else {
//                    headPred = innerMap.get(headAtom.isBoundArray);
//                }
//                var newHead = new Atom(headPred, headAtom.ids);
//                System.out.println("OLD HEAD");
//                System.out.println(headAtom.toString(p));
//                System.out.println("NEW HEAD");
//                System.out.println(newHead.toString(p));
//
//                ArrayList<Atom> newBody = new ArrayList<>(r.body.size());
//                for (var b_A : r.body) {
//                    var bodyAtom = (AdornedAtom) b_A;
//                    long bodyPred;
//                    var bodyInnerMap = adornedToNewId.computeIfAbsent(bodyAtom.pred, x -> new HashMap<>());
//                    System.out.println(bodyInnerMap);
//                    if (!bodyInnerMap.containsKey(bodyAtom.isBoundArray)) {
//                        bodyPred = idCounter++;
//                        idToVar.put(bodyPred, bodyAtom.generatePred(p));
////                        idToVar.put(bodyPred, p.idToVar.get(bodyAtom.pred) + "_" + boolArrToBoundString(bodyAtom.isBoundArray));
//                    } else {
//                        bodyPred = bodyInnerMap.get(bodyAtom.isBoundArray);
//                    }
//                    var newBodyAtom = new Atom(bodyPred, bodyAtom.ids);
//                    newBody.add(newBodyAtom);
//                }
//                var ruleSet = newRules.computeIfAbsent(headPred, x -> new ArrayList<>());
//                ruleSet.add(new Rule(newHead, newBody));
//            }
//        }
////        Handle query
//        var queryAtom = (AdornedAtom) p.query;
//        var innerMap = adornedToNewId.computeIfAbsent(queryAtom.pred, x -> new HashMap<>());
//        long queryPred;
//        if (!innerMap.containsKey(queryAtom.isBoundArray)) {
//            queryPred = idCounter++;
//            idToVar.put(queryPred, queryAtom.generatePred(p));
////            idToVar.put(queryPred, p.idToVar.get(queryAtom.pred) + "_" + boolArrToBoundString(headAtom.isBoundArray));
//        } else {
//            queryPred = innerMap.get(queryAtom.isBoundArray);
//        }
//        var newQuery = new Atom(queryPred, queryAtom.ids);
//
//        return new Program(newFacts, newRules, newQuery, idToVar, idCounter);
//    }

//    private static String boolArrToBoundString(List<Boolean> isBound) {
////        TODO: Right way?
//        return isBound.stream().map(b -> b ? "b" : "f").reduce("", String::concat);
//    }


//    private static Program renamePred(Program p, AdornedAtom query) {
//        var newProgram = new Program(p.facts, null, p.query, p.idToVar, p.nextPred);
//        Map<Long, Map<List<Boolean>, Long>> varToId = new HashMap<>();
//        Map<Long, List<Rule>> newRules = new HashMap<>();
//        for(var ruleEntry: newProgram.rules.entrySet()) {
//            for(var r: ruleEntry.getValue()) {
//                var head = (AdornedAtom) r.head;
//                var innserMap = varToId.computeIfAbsent(head.pred, x -> new HashMap<>());
//                var newPred = innserMap.computeIfAbsent(head.isBoundArray, x -> newProgram.nextPred++);
////                newProgram.idToVar.putIfAbsent();
//                if() {
//
//                }
//            }
//        }
//    }


    private static Tuple<Program, Map<Rule, Set<SIPEntry>>> adornment(Program p) {

        AdornedAtom adornedQuery = adornAtom(p.query);
        Program newProgram = new Program(p.facts, new HashMap<>(), adornedQuery, p.idToVar, p.nextPred);
        Map<Long, Set<List<Boolean>>> workList = new HashMap<>();
        Map<Long, Set<List<Boolean>>> seen = new HashMap<>();

//        Map<Long, Map<List<Boolean>, AdornedAtom>> adornedAtoms = new HashMap<>();
//        Map<List<Boolean>, AdornedAtom> adornedQueryMap = new HashMap<>();
//        adornedQueryMap.put(adornedQuery.adornment, adornedQuery);
//        adornedAtoms.put(adornedQuery.pred, adornedQueryMap);

        Set<List<Boolean>> querySet = new HashSet<>();
        querySet.add(adornedQuery.isBoundArray);
        workList.put(adornedQuery.pred, querySet);

        Set<List<Boolean>> querySetSeen = new HashSet<>();
        querySetSeen.add(adornedQuery.isBoundArray);
        seen.put(adornedQuery.pred, querySetSeen);

        Map<Rule, Set<SIPEntry>> sips = new HashMap<>();


//        Map<AdornedAtom, SIP> sips = new HashMap<>();
        while (!workList.isEmpty()) {
            var it = workList.entrySet().iterator();
            var current = it.next();
            long currentID = current.getKey();
            Set<List<Boolean>> currentSet = current.getValue();
            Iterator<List<Boolean>> it_1 = currentSet.iterator();
            List<Boolean> currentAdornment = it_1.next();
            it_1.remove();
            if (current.getValue().isEmpty()) {
                it.remove();
            }
            for (var r : p.rules.get(currentID)) {
                var adornedAtom = new AdornedAtom(currentID, r.head.ids, currentAdornment);
                Set<SIPEntry> newSIP = new HashSet<>();

                var newRule = createSIP(r, adornedAtom, p, seen, workList, newSIP);
                var ruleSetForId = newProgram.rules.computeIfAbsent(currentID, x -> new ArrayList<>());
                ruleSetForId.add(newRule);
                sips.put(newRule, newSIP);
//                mapForRule.put(currentAdornment, newSIP);
            }
        }
        return new Tuple<>(newProgram, sips);
    }

    private static Rule createSIP(Rule r, AdornedAtom headAtom, Program p, Map<Long, Set<List<Boolean>>> seen, Map<Long, Set<List<Boolean>>> workList, Set<SIPEntry> entries) {
//        Set<SIPEntry> entries = new HashSet<>();
        Atom[] newBody = new Atom[r.body.size()];

        Set<Long> bound = new HashSet<>();
        Map<Long, Set<AdornedAtom>> originExplanation = new HashMap<>();
        Set<Atom> handled = new HashSet<>();
        for (var i = 0; i < r.head.ids.size(); i++) {
            if (headAtom.isBoundArray.get(i)) {
                bound.add(r.head.ids.get(i).value);
                Set<AdornedAtom> s = new HashSet<>();
                s.add(headAtom);
                originExplanation.put(r.head.ids.get(i).value, s);
            }
        }
        while (true) {
            Atom next = null;
            int index = 0;
            boolean foundNewEDB = false;
            boolean foundNewBound = false;
            for (var i = 0; i < r.body.size(); i++) {
                var currentAtom = r.body.get(i);
                if (handled.contains(currentAtom)) {
                    continue;
                }
                var anyBound = currentAtom.ids.stream().anyMatch(x -> !x.isVar || bound.contains(x.value));
                if (anyBound) {
                    next = currentAtom;
                    index = i;
                    foundNewBound = true;
                    break;
                }
                if (p.facts.containsKey(currentAtom.pred)) {
                    if (!foundNewEDB) {
                        foundNewEDB = true;
                        next = currentAtom;
                        index = i;
                    }
                    continue;
                }
                if (next == null) {
                    next = currentAtom;
                    index = i;
                }
            }

            if (next == null) {
                break;
            }
            Set<AdornedAtom> explanationOfNewlyBound = new HashSet<>();
            var inducedAtom = new AdornedAtom(next.pred, next.ids, null);
            if (foundNewBound) {
                HashSet<AdornedAtom> dependencies = new HashSet<>();
//                System.out.println(next.toString(p));
//                next.ids.stream()
//                        .filter(x -> !x.isVar || bound.contains(x.value))
//                        .forEach(x -> System.out.println())
//                        .forEach(System.out::println);
                next.ids.stream()
                        .filter(x -> x.isVar && bound.contains(x.value))
                        .map(x -> originExplanation.get(x.value))
                        .forEach(dependencies::addAll);
                explanationOfNewlyBound.addAll(dependencies);


//                Do not annotate or create SIP for EDB facts
                if (!p.facts.containsKey(next.pred)) {
                    inducedAtom.isBoundArray = next.ids.stream().map(x1 -> !x1.isVar || bound.contains(x1.value)).collect(Collectors.toCollection(ArrayList::new));
                    var newEntry = new SIPEntry(dependencies, inducedAtom);
                    entries.add(newEntry);
                }

//                if (p.facts.containsKey(next.pred)) {
//                    inducedAtom.isBoundArray = null;
//                }
            } else if (!foundNewEDB) {
                inducedAtom.isBoundArray = next.ids.stream().map(x -> true).collect(Collectors.toCollection(ArrayList::new));
            }
//            if (!foundNewEDB && !foundNewBound) {
//                inducedAtom.isBoundArray = next.ids.stream().map(x -> true).collect(Collectors.toCollection(ArrayList::new));
//            }

            if (inducedAtom.isBoundArray != null) {
                var setForPred = seen.computeIfAbsent(inducedAtom.pred, x -> new HashSet<>());
                if (!setForPred.contains(inducedAtom.isBoundArray)) {
                    setForPred.add(inducedAtom.isBoundArray);
                    var workListSet = workList.computeIfAbsent(inducedAtom.pred, x -> new HashSet<>());
                    workListSet.add(inducedAtom.isBoundArray);
                }
//                var possibleSeenBefore = seen.get(inducedAtom.pred);
//                if(possibleSeenBefore == null) {
//
//                }
            }

            explanationOfNewlyBound.add(inducedAtom);
            next.ids.stream()
                    .filter(x -> x.isVar && !bound.contains(x.value))
                    .forEach(x -> {
                        bound.add(x.value);
                        originExplanation.put(x.value, explanationOfNewlyBound);
                    });
            handled.add(next);
            newBody[index] = inducedAtom;
//            newRule.body.set(index, inducedAtom);
        }
        return new Rule(headAtom, new ArrayList<>(Arrays.asList(newBody)));
    }
}

//class SIP {
//    Set<SIPEntry> entries;
//
//    public SIP(Rule r, AdornedAtom headAtom, Program p) {
//
//        }
//
//
//        //            q(x) :- a(x,y), b(x,z), c(z,y)
////            a(x,y). q ->_x a
////            q & a -> y
////            b(x,z). q ->_x b
////            q & b -> z
////            q & a ->_y c
////            q & b ->_z c
//
////            q & a & b ->_{y,z} c
//
//
////            q(x) :- fact(z), c(z,y)
//
//
////            if (foundNewBound) {
////                var adornArr = next.ids.stream().map(x -> !x.isVar || bound.contains(x.value)).collect(Collectors.toCollection(ArrayList::new));
////
//////            q(x) :- a(x,y), b(x,z), c(z,y)
//////            a(x,y). q ->_x a
//////            q & a -> y
//////            b(x,z). q ->_x b
//////            q & b -> z
//////            q & a ->_y c
//////            q & b ->_z c
////
//////            q & a & b ->_{y,z} c
////
////
//////            q(x) :- fact(z), c(z,y)
////
////
////                HashSet<AdornedAtom> dependencies = new HashSet<>();
////                next.ids.stream()
////                        .filter(x -> x.isVar && bound.contains(x.value))
////                        .map(x -> originExplanation.get(x.value))
////                        .forEach(dependencies::addAll);
////
////                var explanationOfNewlyBound = (HashSet<AdornedAtom>) dependencies.clone();
////                var inducedAtom = new AdornedAtom(next.pred, next.ids, adornArr);
////                var newEntry = new SIPEntry(dependencies, inducedAtom);
////                entries.add(newEntry);
////                explanationOfNewlyBound.add(inducedAtom);
////                next.ids.stream()
////                        .filter(x -> x.isVar && !bound.contains(x.value))
////                        .forEach(x -> {
////                            bound.add(x.value);
////                            originExplanation.put(x.value, explanationOfNewlyBound);
////                        });
////                handled.add(next);
////                continue;
////            }
////
////            handled.add(next);
////            var inducedAtom = new AdornedAtom(next.pred, next.ids, null);
////            HashSet<AdornedAtom> explanationOfNewlyBound = new HashSet<>();
////            explanationOfNewlyBound.add(inducedAtom);
////            next.ids.stream()
////                    .filter(x -> x.isVar && !bound.contains(x.value))
////                    .forEach(x -> {
////                        bound.add(x.value);
////                        originExplanation.put(x.value, explanationOfNewlyBound);
////                    });
////
////        }
//
//    }
//}
