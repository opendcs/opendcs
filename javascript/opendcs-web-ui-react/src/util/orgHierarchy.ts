import { ApiOrganization } from "opendcs-api";
import { compareStrings } from "./sort";

export interface OrgTreeEntry {
  org: ApiOrganization;
  /** Depth in the full tree — 0 for a root office. Drives indentation. */
  depth: number;
  /**
   * False for an office that is only present to give a selectable descendant
   * something to hang off of. Callers render these as inert group labels.
   */
  selectable: boolean;
}

type ChildrenByParent = Map<string | null, ApiOrganization[]>;

/**
 * A parent we can't resolve — absent from this list, or an office pointing at
 * itself — is treated as no parent at all, so the office becomes a root rather
 * than dropping out of the list entirely.
 */
const resolveParents = (organizations: ApiOrganization[]) => {
  const known = new Set<string>();
  for (const org of organizations) {
    if (org.name) {
      known.add(org.name);
    }
  }

  const parentOf = new Map<string, string | null>();
  for (const org of organizations) {
    if (!org.name) continue;
    const parent = org.parent;
    const resolvable = parent && parent !== org.name && known.has(parent);
    parentOf.set(org.name, resolvable ? parent : null);
  }
  return parentOf;
};

/**
 * Breaks any report_to cycle the database might hand us: walking up from an
 * office that loops, the first one to detect the loop is promoted to a root.
 * Without this, every office in the cycle is unreachable from the roots and
 * would silently vanish from the menu.
 */
const breakCycles = (parentOf: Map<string, string | null>) => {
  for (const name of [...parentOf.keys()]) {
    const seen = new Set<string>([name]);
    let current = parentOf.get(name) ?? null;
    while (current !== null) {
      if (seen.has(current)) {
        parentOf.set(name, null);
        break;
      }
      seen.add(current);
      current = parentOf.get(current) ?? null;
    }
  }
};

const groupChildren = (
  organizations: ApiOrganization[],
  parentOf: Map<string, string | null>,
): ChildrenByParent => {
  const childrenOf: ChildrenByParent = new Map();
  for (const org of organizations) {
    // An office with no name can't be a parent, so it sits at the root.
    const parent = org.name ? (parentOf.get(org.name) ?? null) : null;
    const siblings = childrenOf.get(parent) ?? [];
    siblings.push(org);
    childrenOf.set(parent, siblings);
  }
  for (const siblings of childrenOf.values()) {
    siblings.sort((a, b) => compareStrings(a.name, b.name));
  }
  return childrenOf;
};

/** An office is kept if it is selectable itself or leads to one. */
const pruneToSelectable = (
  childrenOf: ChildrenByParent,
  isSelectable: (org: ApiOrganization) => boolean,
) => {
  const keep = new Set<ApiOrganization>();
  const retain = (org: ApiOrganization): boolean => {
    const descendants = org.name ? (childrenOf.get(org.name) ?? []) : [];
    // Not short-circuited: every descendant must be visited so the ones that
    // qualify are marked, not just enough of them to prove this branch stays.
    const keptDescendant = descendants.map(retain).some(Boolean);
    if (keptDescendant || isSelectable(org)) {
      keep.add(org);
      return true;
    }
    return false;
  };
  for (const root of childrenOf.get(null) ?? []) {
    retain(root);
  }
  return keep;
};

const flattenDepthFirst = (
  childrenOf: ChildrenByParent,
  keep: Set<ApiOrganization>,
  isSelectable: (org: ApiOrganization) => boolean,
) => {
  const entries: OrgTreeEntry[] = [];
  const walk = (parent: string | null, depth: number) => {
    for (const org of childrenOf.get(parent) ?? []) {
      if (!keep.has(org)) continue;
      entries.push({ org, depth, selectable: isSelectable(org) });
      if (org.name) {
        walk(org.name, depth + 1);
      }
    }
  };
  walk(null, 0);
  return entries;
};

/**
 * Orders organizations as ROOT -> child -> grandchild, siblings alphabetical.
 *
 * CWMS offices form a real tree via report_to_office (SPK -> SPD -> HQ), which
 * arrives here as ApiOrganization.parent. A flat alphabetical list throws away
 * that structure; this keeps it while preserving alphabetical order *within*
 * each set of siblings, which is the ordering users actually scan by.
 *
 * `isSelectable` exists for the org switcher, which only offers offices the
 * user holds a role in. Those are usually leaves, so their parents would be
 * missing and the survivors would render as orphans. Instead the tree is
 * pruned to branches containing at least one selectable office, and the
 * ancestors along the way come back with selectable: false.
 */
export const organizationTree = (
  organizations: ApiOrganization[],
  isSelectable: (org: ApiOrganization) => boolean = () => true,
): OrgTreeEntry[] => {
  const parentOf = resolveParents(organizations);
  breakCycles(parentOf);
  const childrenOf = groupChildren(organizations, parentOf);
  const keep = pruneToSelectable(childrenOf, isSelectable);
  return flattenDepthFirst(childrenOf, keep, isSelectable);
};
