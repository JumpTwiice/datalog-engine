package solver;

import ast.*;
import common.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class Transformer {
//      TODO: Renaming in changeFactsAndRulesToEDGFormat and renamePred could technically cause collisions with already defined names
//       This is not a problem for the solver, but when outputting in readable form it might be impossible to see which is which. Could be solved by appending `@${atom.pread}` to atoms when printing

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
        Program newProgram = new Program(p.facts, ruleMap, adornedProgram.query, adornedProgram.idToVar, adornedProgram.nextPred);
//        changeFactsAndRulesToEDGFormat(newProgram);
        var renamed = renamePred(newProgram, queryMagic);
        changeFactsAndRulesToEDGFormat(renamed);
        return renamed;
    }

    private static Program renamePred(Program p, AdornedAtom magicQuery) {
        var idCounter = 0L;
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

    private static Tuple<Program, Map<Rule, Set<SIPEntry>>> adornment(Program p) {

        AdornedAtom adornedQuery = adornAtom(p.query);
        Program newProgram = new Program(p.facts, new HashMap<>(), adornedQuery, p.idToVar, p.nextPred);
        Map<Long, Set<List<Boolean>>> workList = new HashMap<>();
        Map<Long, Set<List<Boolean>>> seen = new HashMap<>();

        Set<List<Boolean>> querySet = new HashSet<>();
        querySet.add(adornedQuery.isBoundArray);
        workList.put(adornedQuery.pred, querySet);

        Set<List<Boolean>> querySetSeen = new HashSet<>();
        querySetSeen.add(adornedQuery.isBoundArray);
        seen.put(adornedQuery.pred, querySetSeen);

        Map<Rule, Set<SIPEntry>> sips = new HashMap<>();


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
            }
        }
        return new Tuple<>(newProgram, sips);
    }

    private static Rule createSIP(Rule r, AdornedAtom headAtom, Program p, Map<Long, Set<List<Boolean>>> seen, Map<Long, Set<List<Boolean>>> workList, Set<SIPEntry> entries) {
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

            } else if (!foundNewEDB) {
                inducedAtom.isBoundArray = next.ids.stream().map(x -> true).collect(Collectors.toCollection(ArrayList::new));
            }

            if (inducedAtom.isBoundArray != null) {
                var setForPred = seen.computeIfAbsent(inducedAtom.pred, x -> new HashSet<>());
                if (!setForPred.contains(inducedAtom.isBoundArray)) {
                    setForPred.add(inducedAtom.isBoundArray);
                    var workListSet = workList.computeIfAbsent(inducedAtom.pred, x -> new HashSet<>());
                    workListSet.add(inducedAtom.isBoundArray);
                }
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
        }
        return new Rule(headAtom, new ArrayList<>(Arrays.asList(newBody)));
    }
}
