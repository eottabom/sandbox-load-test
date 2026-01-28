FROM golang:1.23-alpine AS builder

RUN go install github.com/perfana/x2i@latest

FROM alpine:3.19

COPY --from=builder /go/bin/x2i /usr/local/bin/x2i

ENTRYPOINT ["x2i"]