target "all" {
    targets = ["lrgs", "compproc", "routingscheduler", "compdepends", "web-api", "migration"]
}

target "docker-metadata-action" {
   tags = ["latest"]
}

target "build" {
    output = ["type=tar,dest=./out.tar"]
    target = "export"
}

target "lrgs" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/lrgs:${tag}"]
    target = "lrgs"
}

target "compproc" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/compproc:${tag}"]
    target = "compproc"
}

target "compdepends" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/compdepends:${tag}"]
    target = "compdepends"
}

target "routingscheduler" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/routingscheduler:${tag}"]
    target = "routingscheduler"
}

target "web-api" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/web-api:${tag}"]
    target = "web-api"
}

target "migration" {
    inherits = ["docker-metadata-action"]
    context = "."
    dockefile = "Dockerfile"
    tags = [for tag in target.docker-metadata-action.tags : "ghcr.io/opendcs/migration:${tag}"]
    target = "migration"
}